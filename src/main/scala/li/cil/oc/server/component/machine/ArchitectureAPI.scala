package li.cil.oc.server.component.machine

import li.cil.oc.api

abstract class ArchitectureAPI(val machine: api.machine.Machine) {
  protected def node = machine.node

  protected def components = machine.components

  def initialize()
}
