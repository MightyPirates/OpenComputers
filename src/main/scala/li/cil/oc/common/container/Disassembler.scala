package li.cil.oc.common.container

import li.cil.oc.common.tileentity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.IInventory
import net.minecraft.inventory.container.ContainerType
import net.minecraft.nbt.CompoundNBT

class Disassembler(selfType: ContainerType[_ <: Disassembler], id: Int, playerInventory: PlayerInventory, val disassembler: IInventory)
  extends Player(selfType, id, playerInventory, disassembler) {

  addSlotToContainer(80, 35, "ocitem")
  addPlayerInventorySlots(8, 84)

  def disassemblyProgress = synchronizedData.getDouble("disassemblyProgress")

  override protected def detectCustomDataChanges(nbt: CompoundNBT): Unit = {
    disassembler match {
      case te: tileentity.Disassembler => synchronizedData.putDouble("disassemblyProgress", te.progress)
      case _ =>
    }
    super.detectCustomDataChanges(nbt)
  }
}
