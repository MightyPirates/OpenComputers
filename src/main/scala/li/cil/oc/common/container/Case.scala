package li.cil.oc.common.container

import li.cil.oc.common.{InventorySlots, tileentity}
import net.minecraft.entity.player.{EntityPlayer, InventoryPlayer}

class Case(playerInventory: InventoryPlayer, computer: tileentity.Case) extends Player(playerInventory, computer) {
  for (i <- 0 to (if (computer.tier >= 2) 2 else 1)) {
    val slot = InventorySlots.computer(computer.tier)(getInventory.size)
    addSlotToContainer(98, 16 + i * slotSize, slot.slot, slot.tier)
  }

  for (i <- 0 to (if (computer.tier == 0) 0 else 1)) {
    val slot = InventorySlots.computer(computer.tier)(getInventory.size)
    addSlotToContainer(120, 16 + (i + 1) * slotSize, slot.slot, slot.tier)
  }

  for (i <- 0 to (if (computer.tier == 0) 0 else 1)) {
    val slot = InventorySlots.computer(computer.tier)(getInventory.size)
    addSlotToContainer(142, 16 + i * slotSize, slot.slot, slot.tier)
  }

  if (computer.tier >= 2) {
    val slot = InventorySlots.computer(computer.tier)(getInventory.size)
    addSlotToContainer(142, 16 + 2 * slotSize, slot.slot)
  }

  {
    val slot = InventorySlots.computer(computer.tier)(getInventory.size)
    addSlotToContainer(120, 16, slot.slot, slot.tier)
  }

  if (computer.tier == 0) {
    val slot = InventorySlots.computer(computer.tier)(getInventory.size)
    addSlotToContainer(120, 16 + 2 * slotSize, slot.slot, slot.tier)
  }

  // Show the player's inventory.
  addPlayerInventorySlots(8, 84)

  override def canInteractWith(player: EntityPlayer) =
    super.canInteractWith(player) && computer.canInteract(player.getCommandSenderName)
}