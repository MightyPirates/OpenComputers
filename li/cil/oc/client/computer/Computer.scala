package li.cil.oc.client.computer

import scala.reflect.runtime.universe._
import li.cil.oc.api.IComputerContext
import li.cil.oc.common.computer.IComputer
import net.minecraft.block.Block
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import li.cil.oc.server.computer.Driver

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

  def component[T: TypeTag](id: Int) = throw new NotImplementedError

  // ----------------------------------------------------------------------- //
  // IComputer
  // ----------------------------------------------------------------------- //

  def add(component: Any, driver: Driver) = false

  def remove(id: Int) = false

  def start() = false

  def stop() {}

  def update() {}

  def readFromNBT(nbt: NBTTagCompound) {}

  def writeToNBT(nbt: NBTTagCompound) {}
}