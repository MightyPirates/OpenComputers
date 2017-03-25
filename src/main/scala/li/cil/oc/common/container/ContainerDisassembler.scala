package li.cil.oc.common.container

import li.cil.oc.common.tileentity
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.nbt.NBTTagCompound

class ContainerDisassembler(playerInventory: InventoryPlayer, val disassembler: tileentity.Disassembler) extends AbstractContainerPlayer(playerInventory, disassembler) {
  addSlotToContainer(80, 35, "ocitem")
  addPlayerInventorySlots(8, 84)

  def disassemblyProgress = synchronizedData.getDouble("disassemblyProgress")

  override protected def detectCustomDataChanges(nbt: NBTTagCompound): Unit = {
    synchronizedData.setDouble("disassemblyProgress", disassembler.progress)
    super.detectCustomDataChanges(nbt)
  }
}
