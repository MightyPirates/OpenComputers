package li.cil.oc.client.computer

import scala.reflect.runtime.universe._

import li.cil.oc.api.IComputerContext
import li.cil.oc.common.computer.IComputer
import net.minecraft.block.Block
import net.minecraft.item.ItemStack
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

  def component[T: TypeTag](id: Int) = throw new NotImplementedError

  // ----------------------------------------------------------------------- //
  // IComputer
  // ----------------------------------------------------------------------- //

  def add(item: ItemStack, id: Int) = None

  def add(block: Block, x: Int, y: Int, z: Int, id: Int) = None

  def remove(id: Int) = false

  def start() = false

  def stop() {}

  def update() {}

  def readFromNBT(nbt: NBTTagCompound) {}

  def writeToNBT(nbt: NBTTagCompound) {}
}