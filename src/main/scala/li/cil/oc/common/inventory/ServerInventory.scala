package li.cil.oc.common.inventory

import li.cil.oc.api.{Driver, Items}
import li.cil.oc.common.{InventorySlots, Slot}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack

trait ServerInventory extends ItemStackInventory {
  def tier: Int

  override def getSizeInventory = InventorySlots.server(tier).length

  override protected def inventoryName = "Server"

  override def getInventoryStackLimit = 1

  override def isUseableByPlayer(player: EntityPlayer) = false

  override def isItemValidForSlot(slot: Int, stack: ItemStack) =
    Option(Driver.driverFor(stack)).fold(false)(driver => {
      val provided = InventorySlots.server(tier)(slot)
      // TODO remove special code in 1.4 when slot type API changes.
      val requiredSlot = Slot.fromApi(driver.slot(stack))
      val isComponentBus = provided.slot == Slot.ComponentBus && {
        val descriptor = Items.get(stack)
        descriptor == Items.get("componentBus1") ||
          descriptor == Items.get("componentBus2") ||
          descriptor == Items.get("componentBus3")
      }
      (requiredSlot == provided.slot || isComponentBus) && driver.tier(stack) <= provided.tier
    })
}
