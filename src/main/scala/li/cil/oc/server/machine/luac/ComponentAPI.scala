package li.cil.oc.server.machine.luac

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

    lua.pushScalaFunction(lua => components.synchronized {
      val address = lua.checkString(1)
      components.get(address) match {
        case name: String =>
          lua.pushInteger(owner.machine.host.componentSlot(address))
          1
        case _ =>
          lua.pushNil()
          lua.pushString("no such component")
          2
      }
    })
    lua.setField(-2, "slot")

    lua.pushScalaFunction(lua => {
      withComponent(lua.checkString(1), component => {
        lua.newTable()
        for ((name, annotation) <- machine.methods(component.getContainer)) {
          lua.pushString(name)
          lua.newTable()
          lua.pushBoolean(annotation.direct)
          lua.setField(-2, "direct")
          lua.pushBoolean(annotation.getter)
          lua.setField(-2, "getter")
          lua.pushBoolean(annotation.setter)
          lua.setField(-2, "setter")
          lua.rawSet(-3)
        }
        1
      })
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
      withComponent(lua.checkString(1), component => {
        val method = lua.checkString(2)
        val methods = machine.methods(component.getContainer)
        owner.documentation(() => Option(methods.get(method)).map(_.doc).orNull)
      })
    })
    lua.setField(-2, "doc")

    lua.setGlobal("component")
  }

  private def withComponent(address: String, f: (ComponentNode) => Int) = Option(node.getNetwork.node(address)) match {
    case Some(component: ComponentNode) if component.canBeSeenFrom(node) || component == node =>
      f(component)
    case _ =>
      lua.pushNil()
      lua.pushString("no such component")
      2
  }
}
