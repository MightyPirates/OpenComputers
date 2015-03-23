package li.cil.oc.common.tileentity

import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import li.cil.oc.common.item.data.PrintData
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.AxisAlignedBB

class Print extends traits.TileEntity {
  val data = new PrintData()

  var boundsOff = unitBounds
  var boundsOn = unitBounds
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
    boundsOff = data.stateOff.drop(1).foldLeft(data.stateOff.headOption.fold(unitBounds)(_.bounds))((a, b) => a.func_111270_a(b.bounds))
    boundsOn = data.stateOn.drop(1).foldLeft(data.stateOn.headOption.fold(unitBounds)(_.bounds))((a, b) => a.func_111270_a(b.bounds))
  }

  private def unitBounds = AxisAlignedBB.getBoundingBox(0, 0, 0, 1, 1, 1)
}
