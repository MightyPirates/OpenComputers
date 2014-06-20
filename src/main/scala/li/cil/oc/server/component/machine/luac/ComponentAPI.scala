package li.cil.oc.server.component.machine.luac

import li.cil.oc.server
import li.cil.oc.server.component.machine.NativeLuaArchitecture
import li.cil.oc.util.ExtendedLuaState.extendLuaState

import scala.collection.convert.WrapAsScala._

class ComponentAPI(owner: NativeLuaArchitecture) extends NativeLuaAPI(owner) {
  def initialize() {
    lua.newTable()

    lua.pushScalaFunction(lua => components.synchronized {
      val filter = if (lua.isString(1)) Option(lua.toString(1)) else None
      val exact = if (lua.isBoolean(2)) lua.toBoolean(2) else true
      lua.newTable(0, components.size)
      def matches(name: String) = if (exact) name == filter.get else name.contains(filter.get)
      for ((address, name) <- components) {
        if (filter.isEmpty || matches(name)) {
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
      owner.invoke(() => machine.invoke(address, method, args.toArray))
    })
    lua.setField(-2, "invoke")

    lua.pushScalaFunction(lua => {
      val address = lua.checkString(1)
      val method = lua.checkString(2)
      owner.documentation(() => machine.documentation(address, method))
    })
    lua.setField(-2, "doc")

    lua.setGlobal("component")
  }
}
