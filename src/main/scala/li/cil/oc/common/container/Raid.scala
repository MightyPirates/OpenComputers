package li.cil.oc.common.container

import li.cil.oc.common.Slot
import li.cil.oc.common.Tier
import li.cil.oc.common.tileentity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.IInventory
import net.minecraft.inventory.container.ContainerType

class Raid(selfType: ContainerType[_ <: Raid], id: Int, playerInventory: PlayerInventory, raid: IInventory)
  extends Player(selfType, id, playerInventory, raid) {

  override protected def getHostClass = classOf[tileentity.Raid]

  addSlotToContainer(60, 23, Slot.HDD, Tier.Three)
  addSlotToContainer(80, 23, Slot.HDD, Tier.Three)
  addSlotToContainer(100, 23, Slot.HDD, Tier.Three)
  addPlayerInventorySlots(8, 84)
}
