package li.cil.oc.server.machine.luaj

import li.cil.oc.Settings
import li.cil.oc.util.ScalaClosure._
import li.cil.repack.org.luaj.vm2.LuaValue
import li.cil.repack.org.luaj.vm2.Varargs

class SystemAPI(owner: LuaJLuaArchitecture) extends LuaJAPI(owner) {
  override def initialize() {
    val system = LuaValue.tableOf()

    // Whether bytecode may be loaded directly.
    system.set("allowBytecode", (_: Varargs) => LuaValue.valueOf(Settings.get.allowBytecode))

    // How long programs may run without yielding before we stop them.
    system.set("timeout", (_: Varargs) => LuaValue.valueOf(Settings.get.timeout))

    // Maximum length of inputs to pattern matcher.
    system.set("maxPatternInputLength", (_: Varargs) => LuaValue.valueOf(Settings.get.maxPatternInputLength))

    lua.set("system", system)
  }
}
