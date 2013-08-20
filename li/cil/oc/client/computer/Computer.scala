package li.cil.oc.client.computer

import li.cil.oc.server.computer.IComputerContext
import net.minecraft.nbt.NBTTagCompound

class Computer(val owner: AnyRef) extends IComputerContext {
  def luaState = null

  def update() {}

  def readFromNBT(nbt: NBTTagCompound) {}

  def writeToNBT(nbt: NBTTagCompound) {}
}