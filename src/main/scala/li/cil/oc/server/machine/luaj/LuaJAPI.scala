package li.cil.oc.server.machine.luaj

import li.cil.oc.server.machine.ArchitectureAPI

abstract class LuaJAPI(val owner: LuaJLuaArchitecture) extends ArchitectureAPI(owner.machine) {
  protected def lua = owner.lua
}
