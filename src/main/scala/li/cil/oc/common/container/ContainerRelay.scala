package li.cil.oc.common.container

import li.cil.oc.common.Slot
import li.cil.oc.common.tileentity
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.nbt.NBTTagCompound

class ContainerRelay(playerInventory: InventoryPlayer, relay: tileentity.Relay) extends AbstractContainerPlayer(playerInventory, relay) {
  addSlotToContainer(151, 15, Slot.CPU)
  addSlotToContainer(151, 34, Slot.Memory)
  addSlotToContainer(151, 53, Slot.HDD)
  addSlotToContainer(178, 15, Slot.Card)
  addPlayerInventorySlots(8, 84)

  def relayDelay = synchronizedData.getInteger("relayDelay")

  def relayAmount = synchronizedData.getInteger("relayAmount")

  def maxQueueSize = synchronizedData.getInteger("maxQueueSize")

  def packetsPerCycleAvg = synchronizedData.getInteger("packetsPerCycleAvg")

  def queueSize = synchronizedData.getInteger("queueSize")

  override protected def detectCustomDataChanges(nbt: NBTTagCompound): Unit = {
    synchronizedData.setInteger("relayDelay", relay.relayDelay)
    synchronizedData.setInteger("relayAmount", relay.relayAmount)
    synchronizedData.setInteger("maxQueueSize", relay.maxQueueSize)
    synchronizedData.setInteger("packetsPerCycleAvg", relay.packetsPerCycleAvg())
    synchronizedData.setInteger("queueSize", relay.queue.size)
    super.detectCustomDataChanges(nbt)
  }
}
