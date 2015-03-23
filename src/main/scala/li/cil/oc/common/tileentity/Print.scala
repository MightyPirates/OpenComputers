package li.cil.oc.common.tileentity

import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import li.cil.oc.common.item.data.PrintData
import li.cil.oc.util.ExtendedAABB
import li.cil.oc.util.ExtendedAABB._
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.nbt.NBTTagCompound

class Print extends traits.TileEntity {
  val data = new PrintData()

  var boundsOff = ExtendedAABB.unitBounds
  var boundsOn = ExtendedAABB.unitBounds
  var state = false

  override def canUpdate: Boolean = false

  override def readFromNBTForServer(nbt: NBTTagCompound): Unit = {
    super.readFromNBTForServer(nbt)
    data.load(nbt.getCompoundTag("data"))
    updateBounds()
  }

  override def writeToNBTForServer(nbt: NBTTagCompound): Unit = {
    super.writeToNBTForServer(nbt)
    nbt.setNewCompoundTag("data", data.save)
  }

  @SideOnly(Side.CLIENT)
  override def readFromNBTForClient(nbt: NBTTagCompound): Unit = {
    super.readFromNBTForClient(nbt)
    data.load(nbt.getCompoundTag("data"))
    updateBounds()
  }

  override def writeToNBTForClient(nbt: NBTTagCompound): Unit = {
    super.writeToNBTForClient(nbt)
    nbt.setNewCompoundTag("data", data.save)
  }

  def updateBounds(): Unit = {
    boundsOff = data.stateOff.drop(1).foldLeft(data.stateOff.headOption.fold(ExtendedAABB.unitBounds)(_.bounds))((a, b) => a.func_111270_a(b.bounds))
    if (boundsOff.volume == 0) boundsOff = ExtendedAABB.unitBounds
    boundsOn = data.stateOn.drop(1).foldLeft(data.stateOn.headOption.fold(ExtendedAABB.unitBounds)(_.bounds))((a, b) => a.func_111270_a(b.bounds))
    if (boundsOn.volume == 0) boundsOn = ExtendedAABB.unitBounds
  }
}
