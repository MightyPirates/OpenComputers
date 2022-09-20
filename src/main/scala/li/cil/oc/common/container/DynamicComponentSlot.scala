package li.cil.oc.common.container

import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.client.Textures
import li.cil.oc.common
import li.cil.oc.common.InventorySlots.InventorySlot
import li.cil.oc.util.InventoryUtils
import li.cil.oc.util.SideTracker
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack
import net.minecraft.util.ResourceLocation

class DynamicComponentSlot(val agentContainer: Player, inventory: IInventory, index: Int, x: Int, y: Int, host: Class[_ <: EnvironmentHost],
    val info: DynamicComponentSlot => InventorySlot, val containerTierGetter: () => Int)
  extends ComponentSlot(inventory, index, x, y, host) {

  override def tier: Int = {
    val mainTier = containerTierGetter()
    if (mainTier >= 0) info(this).tier
    else mainTier
  }

  def tierIcon: ResourceLocation = Textures.Icons.get(tier)

  def slot: String = {
    val mainTier = containerTierGetter()
    if (mainTier >= 0) info(this).slot
    else common.Slot.None
  }

  override def hasBackground: Boolean = Textures.Icons.get(slot) != null

  override def getBackgroundLocation: ResourceLocation = Option(Textures.Icons.get(slot)).getOrElse(super.getBackgroundLocation)

  override def getMaxStackSize: Int =
    slot match {
      case common.Slot.Tool | common.Slot.Any | common.Slot.Filtered => super.getMaxStackSize
      case common.Slot.None => 0
      case _ => 1
    }

  override protected def clearIfInvalid(player: PlayerEntity) {
    if (SideTracker.isServer && hasItem && !mayPlace(getItem)) {
      val stack = getItem
      set(ItemStack.EMPTY)
      InventoryUtils.addToPlayerInventory(stack, player)
    }
  }
}
