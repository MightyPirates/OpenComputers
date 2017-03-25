package li.cil.oc.common.container

import li.cil.oc.common.inventory.DatabaseInventory
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.InventoryPlayer

class ContainerDatabase(playerInventory: InventoryPlayer, databaseInventory: DatabaseInventory) extends AbstractContainerPlayer(playerInventory, databaseInventory) {
  val rows = math.sqrt(databaseInventory.getSizeInventory).ceil.toInt
  val offset = 8 + Array(3, 2, 0)(databaseInventory.tier) * slotSize

  for (row <- 0 until rows; col <- 0 until rows) {
    addSlotToContainer(offset + col * slotSize, offset + row * slotSize)
  }

  // Show the player's inventory.
  addPlayerInventorySlots(8, 174)

  override def canInteractWith(player: EntityPlayer) = player == playerInventory.player
}

