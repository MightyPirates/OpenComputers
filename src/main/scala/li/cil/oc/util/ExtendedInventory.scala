package li.cil.oc.util

import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack

import scala.language.implicitConversions

object ExtendedInventory {

  implicit def extendedInventory(inventory: IInventory): ExtendedInventory = new ExtendedInventory(inventory)

  class ExtendedInventory(val inventory: IInventory) extends Iterable[ItemStack] {
    override def iterator: Iterator[ItemStack] = (for (stack <- (0 until inventory.getSizeInventory).map(inventory.getStackInSlot)) yield stack).iterator
  }

}
