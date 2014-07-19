package li.cil.oc.common.container

import cpw.mods.fml.relauncher.{Side, SideOnly}
import li.cil.oc.common.tileentity
import li.cil.oc.util.SideTracker
import net.minecraft.entity.player.InventoryPlayer

class Disassembler(playerInventory: InventoryPlayer, disassembler: tileentity.Disassembler) extends Player(playerInventory, disassembler) {
  addSlotToContainer(80, 35)
  addPlayerInventorySlots(8, 84)

  var disassemblyProgress = 0.0

  @SideOnly(Side.CLIENT)
  override def updateProgressBar(id: Int, value: Int) {
    super.updateProgressBar(id, value)
    if (id == 0) {
      disassemblyProgress = value / 5.0
    }
  }

  override def detectAndSendChanges() {
    super.detectAndSendChanges()
    if (SideTracker.isServer) {
      if (math.abs(disassembler.progress - disassemblyProgress) > 0.2) {
        disassemblyProgress = disassembler.progress
        sendProgressBarUpdate(0, (disassemblyProgress * 5).toInt)
      }
    }
  }
}
