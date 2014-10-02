package li.cil.oc.server.component.machine.luaj

import li.cil.oc.Settings
import li.cil.oc.server.component.machine.LuaJLuaArchitecture
import li.cil.oc.util.ScalaClosure._
import li.cil.repack.org.luaj.vm2.{LuaValue, Varargs}

class SystemAPI(owner: LuaJLuaArchitecture) extends LuaJAPI(owner) {
  override def initialize() {
    val system = LuaValue.tableOf()

    // Whether bytecode may be loaded directly.
    system.set("allowBytecode", (_: Varargs) => LuaValue.valueOf(Settings.get.allowBytecode))

    // How long programs may run without yielding before we stop them.
    system.set("timeout", (_: Varargs) => LuaValue.valueOf(Settings.get.timeout))

    lua.set("system", system)
  }
}
