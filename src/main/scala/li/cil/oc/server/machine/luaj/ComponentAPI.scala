package li.cil.oc.server.machine.luaj

import li.cil.oc.api.network.Component
import li.cil.oc.util.ScalaClosure._
import li.cil.repack.org.luaj.vm2.LuaValue
import li.cil.repack.org.luaj.vm2.Varargs

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

    component.set("slot", (args: Varargs) => components.synchronized {
      val address = args.checkjstring(1)
      components.get(address) match {
        case name: String =>
          LuaValue.valueOf(machine.host.componentSlot(address))
        case _ =>
          LuaValue.varargsOf(LuaValue.NIL, LuaValue.valueOf("no such component"))
      }
    })

    component.set("methods", (args: Varargs) => {
      withComponent(args.checkjstring(1), component => {
        val table = LuaValue.tableOf()
        for ((name, annotation) <- machine.methods(component.host)) {
          table.set(name, LuaValue.tableOf(Array(
            LuaValue.valueOf("direct"),
            LuaValue.valueOf(annotation.direct),
            LuaValue.valueOf("getter"),
            LuaValue.valueOf(annotation.getter),
            LuaValue.valueOf("setter"),
            LuaValue.valueOf(annotation.setter))))
        }
        table
      })
    })

    component.set("invoke", (args: Varargs) => {
      val address = args.checkjstring(1)
      val method = args.checkjstring(2)
      val params = toSimpleJavaObjects(args, 3)
      owner.invoke(() => machine.invoke(address, method, params.toArray))
    })

    component.set("doc", (args: Varargs) => {
      withComponent(args.checkjstring(1), component => {
        val method = args.checkjstring(2)
        val methods = machine.methods(component.host)
        owner.documentation(() => Option(methods.get(method)).map(_.doc).orNull)
      })
    })

    lua.set("component", component)
  }

  private def withComponent(address: String, f: (Component) => Varargs) = Option(node.network.node(address)) match {
    case Some(component: Component) if component.canBeSeenFrom(node) || component == node =>
      f(component)
    case _ =>
      LuaValue.varargsOf(LuaValue.NIL, LuaValue.valueOf("no such component"))
  }
}
