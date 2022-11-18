package li.cil.oc.common.container

import li.cil.oc.common.Tier
import li.cil.oc.common.tileentity
import li.cil.oc.integration.util.ItemCharge
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.item.ItemStack
import net.minecraft.inventory.IInventory
import net.minecraft.inventory.container.ContainerType

class Charger(selfType: ContainerType[_ <: Charger], id: Int, playerInventory: PlayerInventory, charger: IInventory)
  extends Player(selfType, id, playerInventory, charger) {

  override protected def getHostClass = classOf[tileentity.Charger]

  addSlot(new StaticComponentSlot(this, otherInventory, slots.size, 80, 35, getHostClass, "tablet", Tier.Any) {
    override def mayPlace(stack: ItemStack): Boolean = {
      if (!container.canPlaceItem(getSlotIndex, stack)) return false
      ItemCharge.canCharge(stack)
    }
  })
  addPlayerInventorySlots(8, 84)
}
