package li.cil.oc.common.container

import li.cil.oc.common.tileentity
import net.minecraft.entity.player.InventoryPlayer

class Rack(playerInventory: InventoryPlayer, rack: tileentity.Rack) extends Player(playerInventory, rack) {
  addSlotToContainer(116, 8)
  addSlotToContainer(116, 26)
  addSlotToContainer(116, 44)
  addSlotToContainer(116, 62)
  addPlayerInventorySlots(8, 84)
}
