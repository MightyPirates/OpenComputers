package li.cil.oc.client.computer

import li.cil.oc.api.scala.IComputerContext
import li.cil.oc.common.computer.IComputer
import net.minecraft.nbt.NBTTagCompound

/**
 * This is a dummy class for the client side. It does nothing, really, just
 * saves us a couple of side checks.
 */
class Computer(val owner: AnyRef) extends IComputerContext with IComputer {
  // ----------------------------------------------------------------------- //
  // IComputerContext
  // ----------------------------------------------------------------------- //

  def world = throw new NotImplementedError

  def signal(name: String, args: Any*) = throw new NotImplementedError

  // ----------------------------------------------------------------------- //
  // IComputer
  // ----------------------------------------------------------------------- //

  def start() = false

  def stop() = false

  var isRunning = false

  def update() {}

  def readFromNBT(nbt: NBTTagCompound) {}

  def writeToNBT(nbt: NBTTagCompound) {}
}