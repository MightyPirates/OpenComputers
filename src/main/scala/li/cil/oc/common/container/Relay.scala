package li.cil.oc.common.container

import li.cil.oc.Constants
import li.cil.oc.api
import li.cil.oc.api.detail.ItemInfo
import li.cil.oc.common.Slot
import li.cil.oc.common.Tier
import li.cil.oc.common.tileentity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.item.ItemStack
import net.minecraft.inventory.IInventory
import net.minecraft.inventory.container.ContainerType
import net.minecraft.nbt.CompoundNBT

class Relay(selfType: ContainerType[_ <: Relay], id: Int, playerInventory: PlayerInventory, relay: IInventory)
  extends Player(selfType, id, playerInventory, relay) {

  lazy final val WirelessNetworkCardTier1: ItemInfo = api.Items.get(Constants.ItemName.WirelessNetworkCardTier1)
  lazy final val WirelessNetworkCardTier2: ItemInfo = api.Items.get(Constants.ItemName.WirelessNetworkCardTier2)
  lazy final val LinkedCard: ItemInfo = api.Items.get(Constants.ItemName.LinkedCard)

  addSlotToContainer(151, 15, Slot.CPU)
  addSlotToContainer(151, 34, Slot.Memory)
  addSlotToContainer(151, 53, Slot.HDD)
  addSlot(new StaticComponentSlot(this, otherInventory, slots.size, 178, 15, Slot.Card, Tier.Any) {
    override def mayPlace(stack: ItemStack): Boolean = {
      if (api.Items.get(stack) != WirelessNetworkCardTier1 && api.Items.get(stack) != WirelessNetworkCardTier2 &&
        api.Items.get(stack) != LinkedCard) return false
      super.mayPlace(stack)
    }
  })
  addPlayerInventorySlots(8, 84)

  def relayDelay = synchronizedData.getInt("relayDelay")

  def relayAmount = synchronizedData.getInt("relayAmount")

  def maxQueueSize = synchronizedData.getInt("maxQueueSize")

  def packetsPerCycleAvg = synchronizedData.getInt("packetsPerCycleAvg")

  def queueSize = synchronizedData.getInt("queueSize")

  override protected def detectCustomDataChanges(nbt: CompoundNBT): Unit = {
    relay match {
      case te: tileentity.Relay => {
        synchronizedData.putInt("relayDelay", te.relayDelay)
        synchronizedData.putInt("relayAmount", te.relayAmount)
        synchronizedData.putInt("maxQueueSize", te.maxQueueSize)
        synchronizedData.putInt("packetsPerCycleAvg", te.packetsPerCycleAvg())
        synchronizedData.putInt("queueSize", te.queue.size)
      }
      case _ =>
    }
    super.detectCustomDataChanges(nbt)
  }
}
