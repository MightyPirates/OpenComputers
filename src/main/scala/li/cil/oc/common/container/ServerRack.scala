package li.cil.oc.common.container

import cpw.mods.fml.relauncher.{Side, SideOnly}
import li.cil.oc.common.tileentity
import li.cil.oc.util.SideTracker
import net.minecraft.entity.player.InventoryPlayer

class ServerRack(playerInventory: InventoryPlayer, rack: tileentity.ServerRack) extends Player(playerInventory, rack) {
  addSlotToContainer(106, 8)
  addSlotToContainer(106, 26)
  addSlotToContainer(106, 44)
  addSlotToContainer(106, 62)
  addPlayerInventorySlots(8, 84)

  var lastSentSwitchMode = !rack.internalSwitch

  @SideOnly(Side.CLIENT)
  override def updateProgressBar(id: Int, value: Int) {
    if (id == 0) {
      rack.internalSwitch = value == 1
    }
  }

  override def detectAndSendChanges() {
    super.detectAndSendChanges()
    if (SideTracker.isServer) {
      if (lastSentSwitchMode != rack.internalSwitch) {
        lastSentSwitchMode = rack.internalSwitch
        sendProgressBarUpdate(0, if (lastSentSwitchMode) 1 else 0)
      }
    }
  }
}
