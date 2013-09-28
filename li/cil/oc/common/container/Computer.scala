package li.cil.oc.common.container

import li.cil.oc.common.tileentity.TileEntityComputer
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack

class Computer(playerInventory: InventoryPlayer, computer: TileEntityComputer) extends Player(playerInventory, computer) {
  // PSU
  addSlotToContainer(new Slot(computer, 0, 58, 17) {
    override def isItemValid(item: ItemStack) = {
      computer.isItemValidForSlot(0, item)
    }
  })

  // PCI
  for (i <- 0 to 2) {
    val index = i + 1
    addSlotToContainer(new Slot(computer, index, 80, 17 + i * slotSize) {
      override def isItemValid(item: ItemStack) = {
        computer.isItemValidForSlot(index, item)
      }
    })
  }

  // RAM
  for (i <- 0 to 1) {
    val index = i + 4
    addSlotToContainer(new Slot(computer, index, 102, 17 + i * slotSize) {
      override def isItemValid(item: ItemStack) = {
        computer.isItemValidForSlot(index, item)
      }
    })
  }

  // HDD
  for (i <- 0 to 1) {
    val index = i + 6
    addSlotToContainer(new Slot(computer, index, 124, 17 + i * slotSize) {
      override def isItemValid(item: ItemStack) = {
        computer.isItemValidForSlot(index, item)
      }
    })
  }

  // Show the player's inventory.
  addPlayerInventorySlots(8, 84)
}