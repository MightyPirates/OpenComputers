package li.cil.oc.client.computer

import li.cil.oc.common.computer.IInternalComputerContext
import net.minecraft.nbt.NBTTagCompound

class Computer(val owner: AnyRef) extends IInternalComputerContext {
  def luaState = null

  def start() = false
  
  def stop() {}

  def update() {}

  def signal(name: String, args: Any*) {}

  def readFromNBT(nbt: NBTTagCompound) {}

  def writeToNBT(nbt: NBTTagCompound) {}
}