package li.cil.oc.common.container

import li.cil.oc.common.Slot
import li.cil.oc.common.tileentity
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.nbt.NBTTagCompound

class ContainerPrinter(playerInventory: InventoryPlayer, val printer: tileentity.Printer) extends AbstractContainerPlayer(playerInventory, printer) {
  addSlotToContainer(18, 19, Slot.Filtered)
  addSlotToContainer(18, 51, Slot.Filtered)
  addSlotToContainer(152, 35)

  // Show the player's inventory.
  addPlayerInventorySlots(8, 84)

  def progress = synchronizedData.getDouble("progress")

  def amountMaterial = synchronizedData.getInteger("amountMaterial")

  def amountInk = synchronizedData.getInteger("amountInk")

  override protected def detectCustomDataChanges(nbt: NBTTagCompound): Unit = {
    synchronizedData.setDouble("progress", if (printer.isPrinting) printer.progress / 100.0 else 0)
    synchronizedData.setInteger("amountMaterial", printer.amountMaterial)
    synchronizedData.setInteger("amountInk", printer.amountInk)
    super.detectCustomDataChanges(nbt)
  }
}
