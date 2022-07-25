package li.cil.oc.common.container

import li.cil.oc.common.tileentity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.nbt.CompoundNBT

class Disassembler(id: Int, playerInventory: PlayerInventory, val disassembler: tileentity.Disassembler) extends Player(null, id, playerInventory, disassembler) {
  addSlotToContainer(80, 35, "ocitem")
  addPlayerInventorySlots(8, 84)

  def disassemblyProgress = synchronizedData.getDouble("disassemblyProgress")

  override protected def detectCustomDataChanges(nbt: CompoundNBT): Unit = {
    synchronizedData.putDouble("disassemblyProgress", disassembler.progress)
    super.detectCustomDataChanges(nbt)
  }
}
