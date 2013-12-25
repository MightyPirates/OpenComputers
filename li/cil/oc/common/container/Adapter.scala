package li.cil.oc.common.container

import li.cil.oc.common.tileentity
import net.minecraft.entity.player.{EntityPlayer, InventoryPlayer}
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack

class Adapter(playerInventory: InventoryPlayer, adapter: tileentity.Adapter) extends Player(playerInventory, adapter) {
  addSlotToContainer(new ComponentSlot(otherInventory, 0, 80, 35) {
    override def decrStackSize(amount: Int) = {
      val stack = super.decrStackSize(math.max(amount, 1))
      if (stack == null || stack.stackSize < 1) null
      else stack
    }
  })
  addPlayerInventorySlots(8, 84)

  override def slotClick(index: Int, mouseClick: Int, holdingShift: Int, player: EntityPlayer) = {
    if (index == 0) clearIfNecessary(player.inventory.getItemStack)
    super.slotClick(index, mouseClick, holdingShift, player)
  }

  override protected def tryTransferStackInSlot(from: Slot, offset: Int, length: Int, intoPlayerInventory: Boolean) = {
    if (!intoPlayerInventory) clearIfNecessary(from.getStack)
    super.tryTransferStackInSlot(from, offset, length, intoPlayerInventory)
  }

  private def clearIfNecessary(stack: ItemStack) {
    inventorySlots.get(0) match {
      case slot: Slot if stack == null || slot.isItemValid(stack) => slot.putStack(null)
      case _ =>
    }
  }
}
