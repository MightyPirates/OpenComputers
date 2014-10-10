package li.cil.oc.server.machine.luac

import li.cil.oc.server.machine.ArchitectureAPI

abstract class NativeLuaAPI(val owner: NativeLuaArchitecture) extends ArchitectureAPI(owner.machine) {
  protected def lua = owner.lua
}
