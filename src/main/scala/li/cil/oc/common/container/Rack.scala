package li.cil.oc.common.container

import li.cil.oc.common.tileentity
import net.minecraft.entity.player.InventoryPlayer

class Rack(playerInventory: InventoryPlayer, rack: tileentity.Rack) extends Player(playerInventory, rack) {
  addSlotToContainer(106, 8)
  addSlotToContainer(106, 26)
  addSlotToContainer(106, 44)
  addSlotToContainer(106, 62)
  addPlayerInventorySlots(8, 84)
}
