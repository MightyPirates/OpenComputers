package li.cil.oc.server.component.machine.luaj

import java.io.{IOException, FileNotFoundException}
import java.util.logging.Level
import li.cil.oc.api.machine.LimitReachedException
import li.cil.oc.server.component.machine.LuaJLuaArchitecture
import li.cil.oc.util.ScalaClosure._
import li.cil.oc.{OpenComputers, Settings, server}
import org.luaj.vm3.{Varargs, LuaValue}
import scala.collection.convert.WrapAsScala._

class ComponentAPI(owner: LuaJLuaArchitecture) extends LuaJAPI(owner) {
  override def initialize() {
    // Component interaction stuff.
    val component = LuaValue.tableOf()

    component.set("list", (args: Varargs) => components.synchronized {
      val filter = if (args.isstring(1)) Option(args.tojstring(1)) else None
      val table = LuaValue.tableOf(0, components.size)
      for ((address, name) <- components) {
        if (filter.isEmpty || name.contains(filter.get)) {
          table.set(address, name)
        }
      }
      table
    })

    component.set("type", (args: Varargs) => components.synchronized {
      components.get(args.checkjstring(1)) match {
        case name: String =>
          LuaValue.valueOf(name)
        case _ =>
          LuaValue.varargsOf(LuaValue.NIL, LuaValue.valueOf("no such component"))
      }
    })

    component.set("methods", (args: Varargs) => {
      Option(node.network.node(args.checkjstring(1))) match {
        case Some(component: server.network.Component) if component.canBeSeenFrom(node) || component == node =>
          val table = LuaValue.tableOf()
          for (method <- component.methods()) {
            table.set(method, LuaValue.valueOf(component.isDirect(method)))
          }
          table
        case _ =>
          LuaValue.varargsOf(LuaValue.NIL, LuaValue.valueOf("no such component"))
      }
    })

    component.set("invoke", (args: Varargs) => {
      val address = args.checkjstring(1)
      val method = args.checkjstring(2)
      val params = toSimpleJavaObjects(args, 3)
      try {
        machine.invoke(address, method, params.toArray) match {
          case results: Array[_] =>
            LuaValue.varargsOf(Array(LuaValue.TRUE) ++ results.map(toLuaValue))
          case _ =>
            LuaValue.TRUE
        }
      }
      catch {
        case e: Throwable =>
          if (Settings.get.logLuaCallbackErrors && !e.isInstanceOf[LimitReachedException]) {
            OpenComputers.log.log(Level.WARNING, "Exception in Lua callback.", e)
          }
          e match {
            case _: LimitReachedException =>
              LuaValue.NONE
            case e: IllegalArgumentException if e.getMessage != null =>
              LuaValue.varargsOf(LuaValue.FALSE, LuaValue.valueOf(e.getMessage))
            case e: Throwable if e.getMessage != null =>
              LuaValue.varargsOf(LuaValue.TRUE, LuaValue.NIL, LuaValue.valueOf(e.getMessage))
            case _: IndexOutOfBoundsException =>
              LuaValue.varargsOf(LuaValue.FALSE, LuaValue.valueOf("index out of bounds"))
            case _: IllegalArgumentException =>
              LuaValue.varargsOf(LuaValue.FALSE, LuaValue.valueOf("bad argument"))
            case _: NoSuchMethodException =>
              LuaValue.varargsOf(LuaValue.FALSE, LuaValue.valueOf("no such method"))
            case _: FileNotFoundException =>
              LuaValue.varargsOf(LuaValue.TRUE, LuaValue.NIL, LuaValue.valueOf("file not found"))
            case _: SecurityException =>
              LuaValue.varargsOf(LuaValue.TRUE, LuaValue.NIL, LuaValue.valueOf("access denied"))
            case _: IOException =>
              LuaValue.varargsOf(LuaValue.TRUE, LuaValue.NIL, LuaValue.valueOf("i/o error"))
            case e: Throwable =>
              OpenComputers.log.log(Level.WARNING, "Unexpected error in Lua callback.", e)
              LuaValue.varargsOf(LuaValue.TRUE, LuaValue.NIL, LuaValue.valueOf("unknown error"))
          }
      }
    })

    lua.set("component", component)
  }
}
