package li.cil.oc.server.component.machine

import li.cil.oc.api
import net.minecraft.nbt.NBTTagCompound

abstract class ArchitectureAPI(val machine: api.machine.Machine) {
  protected def node = machine.node

  protected def components = machine.components

  def initialize()

  def load(nbt: NBTTagCompound) {}

  def save(nbt: NBTTagCompound) {}
}
