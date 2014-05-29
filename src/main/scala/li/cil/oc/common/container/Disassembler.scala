package li.cil.oc.common.container

import li.cil.oc.common.tileentity
import net.minecraft.entity.player.InventoryPlayer

class Disassembler(playerInventory: InventoryPlayer, disassembler: tileentity.Disassembler) extends Player(playerInventory, disassembler) {
  addSlotToContainer(80, 35)
  addPlayerInventorySlots(8, 84)
}
