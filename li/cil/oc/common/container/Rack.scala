package li.cil.oc.common.container

import li.cil.oc.common.tileentity
import net.minecraft.entity.player.InventoryPlayer

class Rack(playerInventory: InventoryPlayer, rack: tileentity.Rack) extends Player(playerInventory, rack) {
  addSlotToContainer(80, 35)
  addSlotToContainer(100, 35)
  addSlotToContainer(120, 35)
  addSlotToContainer(140, 35)
  addPlayerInventorySlots(8, 84)
}
