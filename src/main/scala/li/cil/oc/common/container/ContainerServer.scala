package li.cil.oc.common.container

import li.cil.oc.common.InventorySlots
import li.cil.oc.common.inventory.ServerInventory
import li.cil.oc.server.component
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.nbt.NBTTagCompound

class ContainerServer(playerInventory: InventoryPlayer, serverInventory: ServerInventory, val server: Option[component.Server] = None) extends AbstractContainerPlayer(playerInventory, serverInventory) {
  for (i <- 0 to 1) {
    val slot = InventorySlots.server(serverInventory.tier)(getInventory.size)
    addSlotToContainer(76, 7 + i * slotSize, slot.slot, slot.tier)
  }

  val verticalSlots = math.min(3, 1 + serverInventory.tier)
  for (i <- 0 to verticalSlots) {
    val slot = InventorySlots.server(serverInventory.tier)(getInventory.size)
    addSlotToContainer(100, 7 + i * slotSize, slot.slot, slot.tier)
  }

  for (i <- 0 to verticalSlots) {
    val slot = InventorySlots.server(serverInventory.tier)(getInventory.size)
    addSlotToContainer(124, 7 + i * slotSize, slot.slot, slot.tier)
  }

  for (i <- 0 to verticalSlots) {
    val slot = InventorySlots.server(serverInventory.tier)(getInventory.size)
    addSlotToContainer(148, 7 + i * slotSize, slot.slot, slot.tier)
  }

  for (i <- 2 to verticalSlots) {
    val slot = InventorySlots.server(serverInventory.tier)(getInventory.size)
    addSlotToContainer(76, 7 + i * slotSize, slot.slot, slot.tier)
  }

  {
    val slot = InventorySlots.server(serverInventory.tier)(getInventory.size)
    addSlotToContainer(26, 34, slot.slot, slot.tier)
  }

  // Show the player's inventory.
  addPlayerInventorySlots(8, 84)

  override def canInteractWith(player: EntityPlayer) = {
    if (server.isDefined) super.canInteractWith(player)
    else player == playerInventory.player
  }

  var isRunning = false
  var isItem = true

  override def updateCustomData(nbt: NBTTagCompound): Unit = {
    super.updateCustomData(nbt)
    isRunning = nbt.getBoolean("isRunning")
    isItem = nbt.getBoolean("isItem")
  }

  override protected def detectCustomDataChanges(nbt: NBTTagCompound): Unit = {
    super.detectCustomDataChanges(nbt)
    server match {
      case Some(s) => nbt.setBoolean("isRunning", s.machine.isRunning)
      case _ => nbt.setBoolean("isItem", true)
    }
  }
}