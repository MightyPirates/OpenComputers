package li.cil.oc.common.container

import li.cil.oc.client.gui.Icons
import li.cil.oc.common
import li.cil.oc.common.InventorySlots.InventorySlot
import li.cil.oc.util.SideTracker
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.{IInventory, Slot}

class DynamicComponentSlot(val container: Player, inventory: IInventory, index: Int, x: Int, y: Int, val info: Array[Array[InventorySlot]], val tierGetter: () => Int) extends Slot(inventory, index, x, y) with ComponentSlot {
  override def tier = {
    val mainTier = tierGetter()
    if (mainTier >= 0) info(mainTier)(getSlotIndex).tier
    else mainTier
  }

  def tierIcon = Icons.get(tier)

  def slot = {
    val mainTier = tierGetter()
    if (mainTier >= 0) info(tierGetter())(getSlotIndex).slot
    else common.Slot.None
  }

  override def getBackgroundIconIndex = Icons.get(slot)

  override def getSlotStackLimit =
    slot match {
      case common.Slot.Tool | common.Slot.None => super.getSlotStackLimit
      case _ => 1
    }

  override protected def clearIfInvalid(player: EntityPlayer) {
    if (SideTracker.isServer && getHasStack && !isItemValid(getStack)) {
      val stack = getStack
      putStack(null)
      player.inventory.addItemStackToInventory(stack)
      player.dropPlayerItemWithRandomChoice(stack, false)
    }
  }
}
