package li.cil.oc.common.container

import li.cil.oc.common.{InventorySlots, tileentity}
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.inventory.Slot
import li.cil.oc.util.ItemUtils

class RobotAssembler(playerInventory: InventoryPlayer, assembler: tileentity.RobotAssembler) extends Player(playerInventory, assembler) {
  // Computer case.
  addSlotToContainer(12, 12)

  def caseTier = ItemUtils.caseTier(inventorySlots.get(0).asInstanceOf[Slot].getStack)

  // Component containers.
  for (i <- 0 until 3) {
    addSlotToContainer(34 + i * slotSize, 70, InventorySlots.assembler, () => caseTier)
  }

  // Components.
  for (i <- 0 until 9) {
    addSlotToContainer(34 + (i % 3) * slotSize, 12 + (i / 3) * slotSize, InventorySlots.assembler, () => caseTier)
  }

  // Cards.
  for (i <- 0 until 3) {
    addSlotToContainer(104, 12 + i * slotSize, InventorySlots.assembler, () => caseTier)
  }

  // CPU.
  addSlotToContainer(126, 12, InventorySlots.assembler, () => caseTier)

  // RAM.
  for (i <- 0 until 2) {
    addSlotToContainer(126, 30 + i * slotSize, InventorySlots.assembler, () => caseTier)
  }

  // Floppy + HDDs.
  for (i <- 0 until 3) {
    addSlotToContainer(148, 12 + i * slotSize, InventorySlots.assembler, () => caseTier)
  }

  // Show the player's inventory.
  addPlayerInventorySlots(8, 110)
}