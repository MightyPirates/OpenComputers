package li.cil.oc.common.container

import li.cil.oc.common.Slot
import li.cil.oc.common.tileentity
import net.minecraft.entity.player.PlayerInventory

class Adapter(id: Int, playerInventory: PlayerInventory, adapter: tileentity.Adapter) extends Player(null, id, playerInventory, adapter) {
  addSlotToContainer(80, 35, Slot.Upgrade)
  addPlayerInventorySlots(8, 84)
}
