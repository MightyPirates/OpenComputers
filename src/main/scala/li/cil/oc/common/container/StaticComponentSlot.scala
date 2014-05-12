package li.cil.oc.common.container

import li.cil.oc.api
import li.cil.oc.client.gui.Icons
import net.minecraft.inventory.{IInventory, Slot}
import net.minecraft.item.ItemStack

class StaticComponentSlot(val container: Player, inventory: IInventory, index: Int, x: Int, y: Int, val slot: api.driver.Slot, val tier: Int) extends Slot(inventory, index, x, y) with ComponentSlot {
  setBackgroundIcon(Icons.get(slot))

  val tierIcon = Icons.get(tier)

  override def getSlotStackLimit =
    slot match {
      case api.driver.Slot.Tool | api.driver.Slot.None => super.getSlotStackLimit
      case _ => 1
    }

  override def isItemValid(stack: ItemStack) = {
    inventory.isItemValidForSlot(index, stack)
  }
}
