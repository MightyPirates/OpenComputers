package li.cil.oc.common.container

import li.cil.oc.api.Driver
import li.cil.oc.common.Slot
import li.cil.oc.common.Tier
import li.cil.oc.common.tileentity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.item.ItemStack
import net.minecraft.inventory.IInventory
import net.minecraft.inventory.container.ContainerType

class Adapter(selfType: ContainerType[_ <: Adapter], id: Int, playerInventory: PlayerInventory, adapter: IInventory)
  extends Player(selfType, id, playerInventory, adapter) {

  addSlot(new StaticComponentSlot(this, otherInventory, slots.size, 80, 35, Slot.Upgrade, Tier.Any) {
    override def mayPlace(stack: ItemStack): Boolean = {
      if (!container.canPlaceItem(getSlotIndex, stack)) return false
      if (!isActive) return false
      Option(Driver.driverFor(stack, classOf[tileentity.Adapter])) match {
        case Some(driver) => driver.slot(stack) == Slot.Upgrade
        case _ => false
      }
    }
  })
  addPlayerInventorySlots(8, 84)
}
