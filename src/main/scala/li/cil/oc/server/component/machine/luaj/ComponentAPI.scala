package li.cil.oc.server.component.machine.luaj

import li.cil.oc.server
import li.cil.oc.server.component.machine.LuaJLuaArchitecture
import li.cil.oc.util.ScalaClosure._
import org.luaj.vm3.{LuaValue, Varargs}

import scala.collection.convert.WrapAsScala._

class ComponentAPI(owner: LuaJLuaArchitecture) extends LuaJAPI(owner) {
  override def initialize() {
    // Component interaction stuff.
    val component = LuaValue.tableOf()

    component.set("list", (args: Varargs) => components.synchronized {
      val filter = if (args.isstring(1)) Option(args.tojstring(1)) else None
      val exact = args.optboolean(2, false)
      val table = LuaValue.tableOf(0, components.size)
      def matches(name: String) = if (exact) name == filter.get else name.contains(filter.get)
      for ((address, name) <- components) {
        if (filter.isEmpty || matches(name)) {
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
      owner.invoke(() => machine.invoke(address, method, params.toArray))
    })

    component.set("doc", (args: Varargs) => {
      val address = args.checkjstring(1)
      val method = args.checkjstring(2)
      owner.documentation(() => machine.documentation(address, method))
    })

    lua.set("component", component)
  }
}
