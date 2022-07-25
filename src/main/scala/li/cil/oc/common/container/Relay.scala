package li.cil.oc.common.container

import li.cil.oc.common.Slot
import li.cil.oc.common.tileentity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.nbt.CompoundNBT

class Relay(id: Int, playerInventory: PlayerInventory, relay: tileentity.Relay) extends Player(null, id, playerInventory, relay) {
  addSlotToContainer(151, 15, Slot.CPU)
  addSlotToContainer(151, 34, Slot.Memory)
  addSlotToContainer(151, 53, Slot.HDD)
  addSlotToContainer(178, 15, Slot.Card)
  addPlayerInventorySlots(8, 84)

  def relayDelay = synchronizedData.getInt("relayDelay")

  def relayAmount = synchronizedData.getInt("relayAmount")

  def maxQueueSize = synchronizedData.getInt("maxQueueSize")

  def packetsPerCycleAvg = synchronizedData.getInt("packetsPerCycleAvg")

  def queueSize = synchronizedData.getInt("queueSize")

  override protected def detectCustomDataChanges(nbt: CompoundNBT): Unit = {
    synchronizedData.putInt("relayDelay", relay.relayDelay)
    synchronizedData.putInt("relayAmount", relay.relayAmount)
    synchronizedData.putInt("maxQueueSize", relay.maxQueueSize)
    synchronizedData.putInt("packetsPerCycleAvg", relay.packetsPerCycleAvg())
    synchronizedData.putInt("queueSize", relay.queue.size)
    super.detectCustomDataChanges(nbt)
  }
}
