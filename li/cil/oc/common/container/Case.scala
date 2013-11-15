package li.cil.oc.common.container

import li.cil.oc.api
import li.cil.oc.client.gui.Icons
import li.cil.oc.common.tileentity
import net.minecraft.entity.player.{EntityPlayer, InventoryPlayer}
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack

class Case(playerInventory: InventoryPlayer, computer: tileentity.Case) extends Player(playerInventory, computer) {
  addSlotToContainer(new Slot(computer, getInventory.size, 58, 17) {
    setBackgroundIcon(Icons.get(api.driver.Slot.Power))

    override def isItemValid(item: ItemStack) = {
      computer.isItemValidForSlot(0, item)
    }
  })

  for (i <- 0 to 2) {
    val index = getInventory.size
    addSlotToContainer(new Slot(computer, index, 80, 17 + i * slotSize) {
      setBackgroundIcon(Icons.get(api.driver.Slot.Card))

      override def isItemValid(item: ItemStack) = {
        computer.isItemValidForSlot(index, item)
      }
    })
  }

  for (i <- 0 to 1) {
    val index = getInventory.size
    addSlotToContainer(new Slot(computer, index, 102, 17 + i * slotSize) {
      setBackgroundIcon(Icons.get(api.driver.Slot.Memory))

      override def isItemValid(item: ItemStack) = {
        computer.isItemValidForSlot(index, item)
      }
    })
  }

  for (i <- 0 to 1) {
    val index = getInventory.size
    addSlotToContainer(new Slot(computer, index, 124, 17 + i * slotSize) {
      setBackgroundIcon(Icons.get(api.driver.Slot.HardDiskDrive))

      override def isItemValid(item: ItemStack) = {
        computer.isItemValidForSlot(index, item)
      }
    })
  }

  // Show the player's inventory.
  addPlayerInventorySlots(8, 84)

  override def canInteractWith(player: EntityPlayer) =
    super.canInteractWith(player) && computer.isUser(player.getCommandSenderName)
}