package li.cil.oc.common.container

import li.cil.oc.common.Slot
import li.cil.oc.common.tileentity
import net.minecraft.entity.player.InventoryPlayer

class Adapter(playerInventory: InventoryPlayer, adapter: tileentity.TileEntityAdapter) extends Player(playerInventory, adapter) {
  addSlotToContainer(80, 35, Slot.Upgrade)
  addPlayerInventorySlots(8, 84)
}
