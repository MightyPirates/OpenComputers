package li.cil.oc.server.component.machine.luac

import li.cil.oc.server.component.machine.{ArchitectureAPI, NativeLuaArchitecture}

abstract class NativeLuaAPI(val owner: NativeLuaArchitecture) extends ArchitectureAPI(owner.machine) {
  protected def lua = owner.lua
}
