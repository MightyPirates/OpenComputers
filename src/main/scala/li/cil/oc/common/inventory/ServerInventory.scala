package li.cil.oc.common.inventory

import li.cil.oc.api.Driver
import li.cil.oc.api.internal
import li.cil.oc.common.InventorySlots
import li.cil.oc.common.container.ContainerTypes
import li.cil.oc.common.container.{Server => ServerContainer}
import li.cil.oc.util.ItemUtils
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.container.INamedContainerProvider
import net.minecraft.item.ItemStack

trait ServerInventory extends ItemStackInventory with INamedContainerProvider {
  def rackSlot: Int

  def tier: Int = ItemUtils.caseTier(container) max 0

  override def getContainerSize = InventorySlots.server(tier).length

  override protected def inventoryName = "server"

  override def getMaxStackSize = 1

  override def stillValid(player: PlayerEntity) = false

  override def canPlaceItem(slot: Int, stack: ItemStack) =
    Option(Driver.driverFor(stack, classOf[internal.Server])).fold(false)(driver => {
      val provided = InventorySlots.server(tier)(slot)
      driver.slot(stack) == provided.slot && driver.tier(stack) <= provided.tier
    })

  override def createMenu(id: Int, playerInventory: PlayerInventory, player: PlayerEntity) =
    new ServerContainer(ContainerTypes.SERVER, id, playerInventory, container, this, tier, rackSlot)
}
