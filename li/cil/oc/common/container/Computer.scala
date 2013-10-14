package li.cil.oc.common.container

import li.cil.oc.api
import li.cil.oc.client.gui.Icons
import li.cil.oc.common.tileentity
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack

class Computer(playerInventory: InventoryPlayer, computer: tileentity.Computer) extends Player(playerInventory, computer) {
  addSlotToContainer(new Slot(computer, 0, 58, 17) {
    setBackgroundIcon(Icons.bySlotType(api.driver.Slot.Power))

    override def isItemValid(item: ItemStack) = {
      computer.isItemValidForSlot(0, item)
    }
  })

  for (i <- 0 to 2) {
    val index = i + 1
    addSlotToContainer(new Slot(computer, index, 80, 17 + i * slotSize) {
      setBackgroundIcon(Icons.bySlotType(api.driver.Slot.Card))

      override def isItemValid(item: ItemStack) = {
        computer.isItemValidForSlot(index, item)
      }
    })
  }

  for (i <- 0 to 1) {
    val index = i + 4
    addSlotToContainer(new Slot(computer, index, 102, 17 + i * slotSize) {
      setBackgroundIcon(Icons.bySlotType(api.driver.Slot.Memory))

      override def isItemValid(item: ItemStack) = {
        computer.isItemValidForSlot(index, item)
      }
    })
  }

  for (i <- 0 to 1) {
    val index = i + 6
    addSlotToContainer(new Slot(computer, index, 124, 17 + i * slotSize) {
      setBackgroundIcon(Icons.bySlotType(api.driver.Slot.HardDiskDrive))

      override def isItemValid(item: ItemStack) = {
        computer.isItemValidForSlot(index, item)
      }
    })
  }

  // Show the player's inventory.
  addPlayerInventorySlots(8, 84)
}