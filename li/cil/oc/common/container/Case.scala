package li.cil.oc.common.container

import li.cil.oc.api
import li.cil.oc.client.gui.Icons
import li.cil.oc.common.tileentity
import net.minecraft.entity.player.{EntityPlayer, InventoryPlayer}
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack

class Case(playerInventory: InventoryPlayer, `case`: tileentity.Case) extends Player(playerInventory, `case`) {
  addSlotToContainer(new Slot(`case`, getInventory.size, 58, 17) {
    setBackgroundIcon(Icons.get(api.driver.Slot.Power))

    override def isItemValid(item: ItemStack) = {
      `case`.isItemValidForSlot(0, item)
    }
  })

  for (i <- 0 to 2) {
    val index = getInventory.size
    addSlotToContainer(new Slot(`case`, index, 80, 17 + i * slotSize) {
      setBackgroundIcon(Icons.get(api.driver.Slot.Card))

      override def isItemValid(item: ItemStack) = {
        `case`.isItemValidForSlot(index, item)
      }
    })
  }

  for (i <- 0 to 1) {
    val index = getInventory.size
    addSlotToContainer(new Slot(`case`, index, 102, 17 + i * slotSize) {
      setBackgroundIcon(Icons.get(api.driver.Slot.Memory))

      override def isItemValid(item: ItemStack) = {
        `case`.isItemValidForSlot(index, item)
      }
    })
  }

  for (i <- 0 to 1) {
    val index = getInventory.size
    addSlotToContainer(new Slot(`case`, index, 124, 17 + i * slotSize) {
      setBackgroundIcon(Icons.get(api.driver.Slot.HardDiskDrive))

      override def isItemValid(item: ItemStack) = {
        `case`.isItemValidForSlot(index, item)
      }
    })
  }

  // Show the player's inventory.
  addPlayerInventorySlots(8, 84)

  override def canInteractWith(player: EntityPlayer) =
    super.canInteractWith(player) && `case`.computer.isUser(player.getCommandSenderName)
}