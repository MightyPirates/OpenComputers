package li.cil.oc.common.container

import li.cil.oc.common.Slot
import li.cil.oc.common.tileentity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.nbt.CompoundNBT

class Printer(id: Int, playerInventory: PlayerInventory, val printer: tileentity.Printer) extends Player(null, id, playerInventory, printer) {
  addSlotToContainer(18, 19, Slot.Filtered)
  addSlotToContainer(18, 51, Slot.Filtered)
  addSlotToContainer(152, 35)

  // Show the player's inventory.
  addPlayerInventorySlots(8, 84)

  def progress = synchronizedData.getDouble("progress")

  def amountMaterial = synchronizedData.getInt("amountMaterial")

  def amountInk = synchronizedData.getInt("amountInk")

  override protected def detectCustomDataChanges(nbt: CompoundNBT): Unit = {
    synchronizedData.putDouble("progress", if (printer.isPrinting) printer.progress / 100.0 else 0)
    synchronizedData.putInt("amountMaterial", printer.amountMaterial)
    synchronizedData.putInt("amountInk", printer.amountInk)
    super.detectCustomDataChanges(nbt)
  }
}
