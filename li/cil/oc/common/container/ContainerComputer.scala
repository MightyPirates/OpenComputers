package li.cil.oc.common.container

import li.cil.oc.common.tileentity.TileEntityComputer
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack

class ContainerComputer(playerInventory: InventoryPlayer, computer: TileEntityComputer) extends GenericInventoryContainer(playerInventory, computer) {
  // Show the computer's inventory.
  // TODO nicer layout, separate for types, based on background image once it exists
  for (slotY <- 0 until 3) {
    for (slotX <- 0 until 3) {
      val index = slotX + slotY * 3
      val x = 62 + slotX * slotSize
      val y = 17 + slotY * slotSize
      addSlotToContainer(new Slot(computer, index, x, y) {
        override def isItemValid(item: ItemStack) = {
          computer.isItemValidForSlot(index, item)
        }
      })
    }
  }

  // Show the player's inventory.
  addPlayerInventorySlots(8, 84)
}