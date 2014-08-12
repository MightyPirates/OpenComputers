package li.cil.oc.common.container

import li.cil.oc.client.gui.Icons
import li.cil.oc.common
import net.minecraft.inventory.{IInventory, Slot}

class StaticComponentSlot(val container: Player, inventory: IInventory, index: Int, x: Int, y: Int, val slot: String, val tier: Int) extends Slot(inventory, index, x, y) with ComponentSlot {
  setBackgroundIcon(Icons.get(slot))

  val tierIcon = Icons.get(tier)

  override def getSlotStackLimit =
    slot match {
      case common.Slot.Tool | common.Slot.None => super.getSlotStackLimit
      case _ => 1
    }
}
