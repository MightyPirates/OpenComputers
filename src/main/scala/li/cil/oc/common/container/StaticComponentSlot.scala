package li.cil.oc.common.container

import li.cil.oc.client.Textures
import li.cil.oc.common
import net.minecraft.inventory.IInventory

class StaticComponentSlot(val container: AbstractContainerPlayer, inventory: IInventory, index: Int, x: Int, y: Int, val slot: String, val tier: Int) extends ComponentSlot(inventory, index, x, y) {
  if (container.playerInventory.player.getEntityWorld.isRemote) {
    setBackgroundLocation(Textures.Icons.get(slot))
  }

  val tierIcon = Textures.Icons.get(tier)

  override def getSlotStackLimit =
    slot match {
      case common.Slot.Tool | common.Slot.Any | common.Slot.Filtered => super.getSlotStackLimit
      case common.Slot.None => 0
      case _ => 1
    }
}
