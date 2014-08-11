package li.cil.oc.common.container

import cpw.mods.fml.common.FMLCommonHandler
import li.cil.oc.common.{Slot, tileentity}
import net.minecraft.entity.player.InventoryPlayer

class Switch(playerInventory: InventoryPlayer, switch: tileentity.Switch) extends Player(playerInventory, switch) {
  addSlotToContainer(151, 15, Slot.CPU)
  addSlotToContainer(151, 34, Slot.Memory)
  addSlotToContainer(151, 53, Slot.HDD)
  addPlayerInventorySlots(8, 84)

  var relayDelay = 0

  var relayAmount = 0

  var maxQueueSize = 0

  var packetsPerCycleAvg = 0

  var queueSize = 0

  override def updateProgressBar(id: Int, value: Int) {
    super.updateProgressBar(id, value)
    if (id == 0) {
      relayDelay = value
    }
    else if (id == 1) {
      relayAmount = value
    }
    else if (id == 2) {
      maxQueueSize = value
    }
    else if (id == 3) {
      packetsPerCycleAvg = value
    }
    else if (id == 4) {
      queueSize = value
    }
  }

  override def detectAndSendChanges() {
    super.detectAndSendChanges()
    if (FMLCommonHandler.instance.getEffectiveSide.isServer) {
      if (switch.relayDelay != relayDelay) {
        relayDelay = switch.relayDelay
        sendProgressBarUpdate(0, relayDelay)
      }
      if (switch.relayAmount != relayAmount) {
        relayAmount = switch.relayAmount
        sendProgressBarUpdate(1, relayAmount)
      }
      if (switch.maxQueueSize != maxQueueSize) {
        maxQueueSize = switch.maxQueueSize
        sendProgressBarUpdate(2, maxQueueSize)
      }
      if (switch.packetsPerCycleAvg() != packetsPerCycleAvg) {
        packetsPerCycleAvg = switch.packetsPerCycleAvg()
        sendProgressBarUpdate(3, packetsPerCycleAvg)
      }
      if (switch.queue.size != queueSize) {
        queueSize = switch.queue.size
        sendProgressBarUpdate(4, queueSize)
      }
    }
  }
}
