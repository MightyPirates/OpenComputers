package li.cil.oc.server.component.machine.luac

import com.google.common.base.Strings
import java.io.{IOException, FileNotFoundException}
import java.util.logging.Level
import li.cil.oc.api.machine.LimitReachedException
import li.cil.oc.server.component.machine.NativeLuaArchitecture
import li.cil.oc.util.ExtendedLuaState.extendLuaState
import li.cil.oc.{OpenComputers, Settings, server}
import scala.collection.convert.WrapAsScala._

class ComponentAPI(owner: NativeLuaArchitecture) extends NativeLuaAPI(owner) {
  def initialize() {
    lua.newTable()

    lua.pushScalaFunction(lua => components.synchronized {
      val filter = if (lua.isString(1)) Option(lua.toString(1)) else None
      lua.newTable(0, components.size)
      for ((address, name) <- components) {
        if (filter.isEmpty || name.contains(filter.get)) {
          lua.pushString(address)
          lua.pushString(name)
          lua.rawSet(-3)
        }
      }
      1
    })
    lua.setField(-2, "list")

    lua.pushScalaFunction(lua => components.synchronized {
      components.get(lua.checkString(1)) match {
        case name: String =>
          lua.pushString(name)
          1
        case _ =>
          lua.pushNil()
          lua.pushString("no such component")
          2
      }
    })
    lua.setField(-2, "type")

    lua.pushScalaFunction(lua => {
      Option(node.network.node(lua.checkString(1))) match {
        case Some(component: server.network.Component) if component.canBeSeenFrom(node) || component == node =>
          lua.newTable()
          for (method <- component.methods()) {
            lua.pushString(method)
            lua.pushBoolean(component.isDirect(method))
            lua.rawSet(-3)
          }
          1
        case _ =>
          lua.pushNil()
          lua.pushString("no such component")
          2
      }
    })
    lua.setField(-2, "methods")

    lua.pushScalaFunction(lua => {
      val address = lua.checkString(1)
      val method = lua.checkString(2)
      val args = lua.toSimpleJavaObjects(3)
      try {
        machine.invoke(address, method, args.toArray) match {
          case results: Array[_] =>
            lua.pushBoolean(true)
            results.foreach(result => lua.pushValue(result))
            1 + results.length
          case _ =>
            lua.pushBoolean(true)
            1
        }
      }
      catch {
        case e: Throwable =>
          if (Settings.get.logLuaCallbackErrors && !e.isInstanceOf[LimitReachedException]) {
            OpenComputers.log.log(Level.WARNING, "Exception in Lua callback.", e)
          }
          e match {
            case _: LimitReachedException =>
              0
            case e: IllegalArgumentException if e.getMessage != null =>
              lua.pushBoolean(false)
              lua.pushString(e.getMessage)
              2
            case e: Throwable if e.getMessage != null =>
              lua.pushBoolean(true)
              lua.pushNil()
              lua.pushString(e.getMessage)
              if (Settings.get.logLuaCallbackErrors) {
                lua.pushString(e.getStackTraceString.replace("\r\n", "\n"))
                4
              }
              else 3
            case _: IndexOutOfBoundsException =>
              lua.pushBoolean(false)
              lua.pushString("index out of bounds")
              2
            case _: IllegalArgumentException =>
              lua.pushBoolean(false)
              lua.pushString("bad argument")
              2
            case _: NoSuchMethodException =>
              lua.pushBoolean(false)
              lua.pushString("no such method")
              2
            case _: FileNotFoundException =>
              lua.pushBoolean(true)
              lua.pushNil()
              lua.pushString("file not found")
              3
            case _: SecurityException =>
              lua.pushBoolean(true)
              lua.pushNil()
              lua.pushString("access denied")
              3
            case _: IOException =>
              lua.pushBoolean(true)
              lua.pushNil()
              lua.pushString("i/o error")
              3
            case e: Throwable =>
              OpenComputers.log.log(Level.WARNING, "Unexpected error in Lua callback.", e)
              lua.pushBoolean(true)
              lua.pushNil()
              lua.pushString("unknown error")
              3
          }
      }
    })
    lua.setField(-2, "invoke")

    lua.pushScalaFunction(lua => {
      val address = lua.checkString(1)
      val method = lua.checkString(2)
      try {
        val doc = machine.documentation(address, method)
        if (Strings.isNullOrEmpty(doc))
          lua.pushNil()
        else
          lua.pushString(doc)
        1
      } catch {
        case e: NoSuchMethodException =>
          lua.pushNil()
          lua.pushString("no such method")
          2
        case t: Throwable =>
          lua.pushNil()
          lua.pushString(if (t.getMessage != null) t.getMessage else t.toString)
          2
      }
    })
    lua.setField(-2, "doc")

    lua.setGlobal("component")
  }
}
