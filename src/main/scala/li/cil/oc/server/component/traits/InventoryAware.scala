package li.cil.oc.server.component.traits

import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack

trait InventoryAware {
  def inventory: IInventory

  def selectedSlot: Int

  def selectedSlot_=(value: Int): Unit

  def insertionSlots = {
    val slots = (0 until inventory.getSizeInventory).toIterable
    slots.drop(selectedSlot) ++ slots.take(selectedSlot)
  }

  // ----------------------------------------------------------------------- //

  protected def stackInSlot(slot: Int) = Option(inventory.getStackInSlot(slot))

  protected def haveSameItemType(stackA: ItemStack, stackB: ItemStack) =
    stackA.getItem == stackB.getItem &&
      (!stackA.getHasSubtypes || stackA.getItemDamage == stackB.getItemDamage)
}
