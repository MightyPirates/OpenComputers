package li.cil.oc.common.container

import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import li.cil.oc.common.tileentity
import li.cil.oc.util.SideTracker
import net.minecraft.entity.player.InventoryPlayer

class Printer(playerInventory: InventoryPlayer, val printer: tileentity.Printer) extends Player(playerInventory, printer) {
  addSlotToContainer(18, 19)
  addSlotToContainer(18, 51)
  addSlotToContainer(152, 35)

  // Show the player's inventory.
  addPlayerInventorySlots(8, 84)

  var isAssembling = false
  var assemblyProgress = 0.0
  var assemblyRemainingTime = 0

  @SideOnly(Side.CLIENT)
  override def updateProgressBar(id: Int, value: Int) {
    super.updateProgressBar(id, value)
    if (id == 0) {
      isAssembling = value == 1
    }

    if (id == 1) {
      assemblyProgress = value / 5.0
    }

    if (id == 2) {
      assemblyRemainingTime = value
    }
  }

  override def detectAndSendChanges() {
    super.detectAndSendChanges()
    if (SideTracker.isServer) {
      if (isAssembling != printer.isAssembling) {
        isAssembling = printer.isAssembling
        sendProgressBarUpdate(0, if (isAssembling) 1 else 0)
      }
      val timeRemaining = printer.timeRemaining
      if (math.abs(printer.progress - assemblyProgress) > 0.2 || assemblyRemainingTime != timeRemaining) {
        assemblyProgress = printer.progress
        assemblyRemainingTime = timeRemaining
        sendProgressBarUpdate(1, (assemblyProgress * 5).toInt)
        sendProgressBarUpdate(2, timeRemaining)
      }
    }
  }
}