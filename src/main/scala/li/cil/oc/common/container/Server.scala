package li.cil.oc.common.container

import li.cil.oc.common.InventorySlots
import li.cil.oc.common.inventory.ServerInventory
import li.cil.oc.server.component
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.IInventory
import net.minecraft.inventory.container.ContainerType
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundNBT

class Server(selfType: ContainerType[_ <: Server], id: Int, playerInventory: PlayerInventory, val stack: ItemStack, serverInventory: IInventory, tier: Int, val rackSlot: Int)
  extends Player(selfType, id, playerInventory, serverInventory) {

  def this(selfType: ContainerType[_ <: Server], id: Int, playerInventory: PlayerInventory, serverInventory: ServerInventory, rackSlot: Int = -1) =
    this(selfType, id, playerInventory, serverInventory.container, serverInventory, serverInventory.tier, rackSlot)

  for (i <- 0 to 1) {
    val slot = InventorySlots.server(tier)(slots.size)
    addSlotToContainer(76, 7 + i * slotSize, slot.slot, slot.tier)
  }

  val verticalSlots = math.min(3, 1 + tier)
  for (i <- 0 to verticalSlots) {
    val slot = InventorySlots.server(tier)(slots.size)
    addSlotToContainer(100, 7 + i * slotSize, slot.slot, slot.tier)
  }

  for (i <- 0 to verticalSlots) {
    val slot = InventorySlots.server(tier)(slots.size)
    addSlotToContainer(124, 7 + i * slotSize, slot.slot, slot.tier)
  }

  for (i <- 0 to verticalSlots) {
    val slot = InventorySlots.server(tier)(slots.size)
    addSlotToContainer(148, 7 + i * slotSize, slot.slot, slot.tier)
  }

  for (i <- 2 to verticalSlots) {
    val slot = InventorySlots.server(tier)(slots.size)
    addSlotToContainer(76, 7 + i * slotSize, slot.slot, slot.tier)
  }

  {
    val slot = InventorySlots.server(tier)(slots.size)
    addSlotToContainer(26, 34, slot.slot, slot.tier)
  }

  // Show the player's inventory.
  addPlayerInventorySlots(8, 84)

  override def stillValid(player: PlayerEntity) = {
    otherInventory match {
      case _: component.Server => super.stillValid(player)
      case _ => player == playerInventory.player
    }
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
    otherInventory match {
      case s: component.Server => nbt.putBoolean("isRunning", s.machine.isRunning)
      case _ => nbt.putBoolean("isItem", true)
    }
  }
}