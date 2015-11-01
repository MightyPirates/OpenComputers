package li.cil.oc.common.container

import li.cil.oc.common.Slot
import li.cil.oc.common.tileentity
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagIntArray
import net.minecraftforge.common.util.Constants.NBT
import net.minecraftforge.common.util.ForgeDirection

class Rack(playerInventory: InventoryPlayer, rack: tileentity.Rack) extends Player(playerInventory, rack) {
  addSlotToContainer(106, 8, Slot.RackMountable)
  addSlotToContainer(106, 26, Slot.RackMountable)
  addSlotToContainer(106, 44, Slot.RackMountable)
  addSlotToContainer(106, 62, Slot.RackMountable)
  addPlayerInventorySlots(8, 84)

  override def updateCustomData(nbt: NBTTagCompound): Unit = {
    super.updateCustomData(nbt)
    nbt.setNewTagList("nodeMapping", rack.nodeMapping.map(sides => toNbt(sides.map {
      case Some(side) => side.ordinal()
      case _ => -1
    })))
    nbt.setBoolean("isRelayEnabled", rack.isRelayEnabled)
  }

  override protected def detectCustomDataChanges(nbt: NBTTagCompound): Unit = {
    super.detectCustomDataChanges(nbt)
    nbt.getTagList("nodeMapping", NBT.TAG_INT_ARRAY).map((sides: NBTTagIntArray) => {
      sides.func_150302_c().map(side => if (side >= 0) Option(ForgeDirection.getOrientation(side)) else None)
    }).copyToArray(rack.nodeMapping)
    rack.isRelayEnabled = nbt.getBoolean("isRelayEnabled")
  }
}
