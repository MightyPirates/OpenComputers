package li.cil.oc.common.container

import li.cil.oc.common.InventorySlots
import li.cil.oc.common.inventory.ServerInventory
import li.cil.oc.server.component
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.nbt.CompoundNBT

class Server(id: Int, playerInventory: PlayerInventory, serverInventory: ServerInventory, val server: Option[component.Server] = None) extends Player(null, id, playerInventory, serverInventory) {
  for (i <- 0 to 1) {
    val slot = InventorySlots.server(serverInventory.tier)(getItems.size)
    addSlotToContainer(76, 7 + i * slotSize, slot.slot, slot.tier)
  }

  val verticalSlots = math.min(3, 1 + serverInventory.tier)
  for (i <- 0 to verticalSlots) {
    val slot = InventorySlots.server(serverInventory.tier)(getItems.size)
    addSlotToContainer(100, 7 + i * slotSize, slot.slot, slot.tier)
  }

  for (i <- 0 to verticalSlots) {
    val slot = InventorySlots.server(serverInventory.tier)(getItems.size)
    addSlotToContainer(124, 7 + i * slotSize, slot.slot, slot.tier)
  }

  for (i <- 0 to verticalSlots) {
    val slot = InventorySlots.server(serverInventory.tier)(getItems.size)
    addSlotToContainer(148, 7 + i * slotSize, slot.slot, slot.tier)
  }

  for (i <- 2 to verticalSlots) {
    val slot = InventorySlots.server(serverInventory.tier)(getItems.size)
    addSlotToContainer(76, 7 + i * slotSize, slot.slot, slot.tier)
  }

  {
    val slot = InventorySlots.server(serverInventory.tier)(getItems.size)
    addSlotToContainer(26, 34, slot.slot, slot.tier)
  }

  // Show the player's inventory.
  addPlayerInventorySlots(8, 84)

  override def stillValid(player: PlayerEntity) = {
    if (server.isDefined) super.stillValid(player)
    else player == playerInventory.player
  }

  var isRunning = false
  var isItem = true

  override def updateCustomData(nbt: CompoundNBT): Unit = {
    super.updateCustomData(nbt)
    isRunning = nbt.getBoolean("isRunning")
    isItem = nbt.getBoolean("isItem")
  }

  override protected def detectCustomDataChanges(nbt: CompoundNBT): Unit = {
    super.detectCustomDataChanges(nbt)
    server match {
      case Some(s) => nbt.putBoolean("isRunning", s.machine.isRunning)
      case _ => nbt.putBoolean("isItem", true)
    }
  }
}