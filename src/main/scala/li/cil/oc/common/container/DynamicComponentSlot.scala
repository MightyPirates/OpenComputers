package li.cil.oc.common.container

import li.cil.oc.client.Textures
import li.cil.oc.common
import li.cil.oc.common.InventorySlots.InventorySlot
import li.cil.oc.util.InventoryUtils
import li.cil.oc.util.SideTracker
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack
import net.minecraft.util.ResourceLocation

class DynamicComponentSlot(val container: Player, inventory: IInventory, index: Int, x: Int, y: Int, val info: DynamicComponentSlot => InventorySlot, val containerTierGetter: () => Int) extends ComponentSlot(inventory, index, x, y) {
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

  override def getSlotStackLimit: Int =
    slot match {
      case common.Slot.Tool | common.Slot.Any | common.Slot.Filtered => super.getSlotStackLimit
      case common.Slot.None => 0
      case _ => 1
    }

  override protected def clearIfInvalid(player: EntityPlayer) {
    if (SideTracker.isServer && getHasStack && !isItemValid(getStack)) {
      val stack = getStack
      putStack(ItemStack.EMPTY)
      InventoryUtils.addToPlayerInventory(stack, player)
    }
  }
}
