package li.cil.oc.common.container

import li.cil.oc.common.InventorySlots
import li.cil.oc.common.Tier
import li.cil.oc.common.tileentity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.util.text.ITextComponent

class Case(id: Int, playerInventory: PlayerInventory, computer: tileentity.Case) extends Player(null, id, playerInventory, computer) {
  for (i <- 0 to (if (computer.tier >= Tier.Three) 2 else 1)) {
    val slot = InventorySlots.computer(computer.tier)(getItems.size)
    addSlotToContainer(98, 16 + i * slotSize, slot.slot, slot.tier)
  }

  for (i <- 0 to (if (computer.tier == Tier.One) 0 else 1)) {
    val slot = InventorySlots.computer(computer.tier)(getItems.size)
    addSlotToContainer(120, 16 + (i + 1) * slotSize, slot.slot, slot.tier)
  }

  for (i <- 0 to (if (computer.tier == Tier.One) 0 else 1)) {
    val slot = InventorySlots.computer(computer.tier)(getItems.size)
    addSlotToContainer(142, 16 + i * slotSize, slot.slot, slot.tier)
  }

  if (computer.tier >= Tier.Three) {
    val slot = InventorySlots.computer(computer.tier)(getItems.size)
    addSlotToContainer(142, 16 + 2 * slotSize, slot.slot, slot.tier)
  }

  {
    val slot = InventorySlots.computer(computer.tier)(getItems.size)
    addSlotToContainer(120, 16, slot.slot, slot.tier)
  }

  if (computer.tier == Tier.One) {
    val slot = InventorySlots.computer(computer.tier)(getItems.size)
    addSlotToContainer(120, 16 + 2 * slotSize, slot.slot, slot.tier)
  }

  {
    val slot = InventorySlots.computer(computer.tier)(getItems.size)
    addSlotToContainer(48, 34, slot.slot, slot.tier)
  }

  // Show the player's inventory.
  addPlayerInventorySlots(8, 84)

  override def stillValid(player: PlayerEntity) =
    super.stillValid(player) && computer.canInteract(player.getName.getString)
}