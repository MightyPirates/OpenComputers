package li.cil.oc.common.container

import li.cil.oc.client.Textures
import li.cil.oc.common
import li.cil.oc.common.InventorySlots.InventorySlot
import li.cil.oc.util.InventoryUtils
import li.cil.oc.util.SideTracker
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.IInventory

class DynamicComponentSlot(val container: AbstractContainerPlayer, inventory: IInventory, index: Int, x: Int, y: Int, val info: DynamicComponentSlot => InventorySlot, val containerTierGetter: () => Int) extends ComponentSlot(inventory, index, x, y) {
  override def tier = {
    val mainTier = containerTierGetter()
    if (mainTier >= 0) info(this).tier
    else mainTier
  }

  def tierIcon = Textures.Icons.get(tier)

  def slot = {
    val mainTier = containerTierGetter()
    if (mainTier >= 0) info(this).slot
    else common.Slot.None
  }

  override def hasBackground = Textures.Icons.get(slot) != null

  override def getBackgroundLocation = Option(Textures.Icons.get(slot)).getOrElse(super.getBackgroundLocation)

  override def getSlotStackLimit =
    slot match {
      case common.Slot.Tool | common.Slot.Any | common.Slot.Filtered => super.getSlotStackLimit
      case common.Slot.None => 0
      case _ => 1
    }

  override protected def clearIfInvalid(player: EntityPlayer) {
    if (SideTracker.isServer && getHasStack && !isItemValid(getStack)) {
      val stack = getStack
      putStack(null)
      InventoryUtils.addToPlayerInventory(stack, player)
    }
  }
}
