package li.cil.oc.common.container

import li.cil.oc.common.tileentity
import li.cil.oc.util.SideTracker
import net.minecraft.entity.player.InventoryPlayer
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

class ServerRack(playerInventory: InventoryPlayer, rack: tileentity.ServerRack) extends Player(playerInventory, rack) {
  addSlotToContainer(106, 8, "server")
  addSlotToContainer(106, 26, "server")
  addSlotToContainer(106, 44, "server")
  addSlotToContainer(106, 62, "server")
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
