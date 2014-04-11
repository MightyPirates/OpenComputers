package li.cil.oc.server.component.machine.luaj

import li.cil.oc.server.component.machine.{ArchitectureAPI, LuaJLuaArchitecture}

abstract class LuaJAPI(val owner: LuaJLuaArchitecture) extends ArchitectureAPI(owner.machine) {
  protected def lua = owner.lua
}
