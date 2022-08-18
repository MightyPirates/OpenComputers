package li.cil.oc.common.container

import li.cil.oc.common.Slot
import li.cil.oc.common.tileentity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.IInventory
import net.minecraft.inventory.container.ContainerType
import net.minecraft.nbt.CompoundNBT

class Printer(selfType: ContainerType[_ <: Printer], id: Int, playerInventory: PlayerInventory, val printer: IInventory)
  extends Player(selfType, id, playerInventory, printer) {

  addSlotToContainer(18, 19, Slot.Filtered)
  addSlotToContainer(18, 51, Slot.Filtered)
  addSlotToContainer(152, 35)

  // Show the player's inventory.
  addPlayerInventorySlots(8, 84)

  def progress = synchronizedData.getDouble("progress")

  def maxAmountMaterial = synchronizedData.getInt("maxAmountMaterial")

  def amountMaterial = synchronizedData.getInt("amountMaterial")

  def maxAmountInk = synchronizedData.getInt("maxAmountInk")

  def amountInk = synchronizedData.getInt("amountInk")

  override protected def detectCustomDataChanges(nbt: CompoundNBT): Unit = {
    printer match {
      case te: tileentity.Printer => {
        synchronizedData.putDouble("progress", if (te.isPrinting) te.progress / 100.0 else 0)
        synchronizedData.putInt("maxAmountMaterial", te.maxAmountMaterial)
        synchronizedData.putInt("amountMaterial", te.amountMaterial)
        synchronizedData.putInt("maxAmountInk", te.amountInk)
        synchronizedData.putInt("amountInk", te.amountInk)
      }
      case _ =>
    }
    super.detectCustomDataChanges(nbt)
  }
}
