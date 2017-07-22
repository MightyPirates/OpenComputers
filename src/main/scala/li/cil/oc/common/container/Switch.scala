package li.cil.oc.common.container

import li.cil.oc.common.Slot
import li.cil.oc.common.tileentity
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.nbt.NBTTagCompound

// TODO Remove in 1.7
class Switch(playerInventory: InventoryPlayer, switch: tileentity.Switch) extends Player(playerInventory, switch) {
  addSlotToContainer(151, 15, Slot.CPU)
  addSlotToContainer(151, 34, Slot.Memory)
  addSlotToContainer(151, 53, Slot.HDD)
  addPlayerInventorySlots(8, 84)

  def relayDelay = synchronizedData.getInteger("relayDelay")

  def relayAmount = synchronizedData.getInteger("relayAmount")

  def maxQueueSize = synchronizedData.getInteger("maxQueueSize")

  def packetsPerCycleAvg = synchronizedData.getInteger("packetsPerCycleAvg")

  def queueSize = synchronizedData.getInteger("queueSize")

  override protected def detectCustomDataChanges(nbt: NBTTagCompound): Unit = {
    synchronizedData.setInteger("relayDelay", switch.relayDelay)
    synchronizedData.setInteger("relayAmount", switch.relayAmount)
    synchronizedData.setInteger("maxQueueSize", switch.maxQueueSize)
    synchronizedData.setInteger("packetsPerCycleAvg", switch.packetsPerCycleAvg())
    synchronizedData.setInteger("queueSize", switch.queue.size)
    super.detectCustomDataChanges(nbt)
  }
}
