package li.cil.oc.common.container

import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import li.cil.oc.common.Slot
import li.cil.oc.common.tileentity
import li.cil.oc.util.SideTracker
import net.minecraft.entity.player.InventoryPlayer

class Printer(playerInventory: InventoryPlayer, val printer: tileentity.Printer) extends Player(playerInventory, printer) {
  addSlotToContainer(18, 19, Slot.Filtered)
  addSlotToContainer(18, 51, Slot.Filtered)
  addSlotToContainer(152, 35)

  // Show the player's inventory.
  addPlayerInventorySlots(8, 84)

  var isPrinting = false
  var amountPlastic = 0
  var amountInk = 0

  @SideOnly(Side.CLIENT)
  override def updateProgressBar(id: Int, value: Int) {
    super.updateProgressBar(id, value)
    if (id == 0) {
      isPrinting = value == 1
    }

    if (id == 1) {
      amountPlastic = value
    }

    if (id == 2) {
      amountInk = value
    }
  }

  override def detectAndSendChanges() {
    super.detectAndSendChanges()
    if (SideTracker.isServer) {
      if (isPrinting != printer.isPrinting) {
        isPrinting = printer.isPrinting
        sendProgressBarUpdate(0, if (isPrinting) 1 else 0)
      }
      if (amountPlastic != printer.amountPlastic) {
        amountPlastic = printer.amountPlastic
        sendProgressBarUpdate(1, amountPlastic)
      }
      if (amountInk != printer.amountInk) {
        amountInk = printer.amountInk
        sendProgressBarUpdate(2, amountInk)
      }
    }
  }
}