package li.cil.oc.common.tileentity

import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity

class TileEntityComputer(isClient: Boolean) extends TileEntity {
  // ----------------------------------------------------------------------- //
  // General
  // ----------------------------------------------------------------------- //

  private val computer =
    if (isClient) new li.cil.oc.client.computer.Computer(this)
    else new li.cil.oc.server.computer.Computer(this)

  override def readFromNBT(nbt: NBTTagCompound) = {
    super.readFromNBT(nbt)
    computer.readFromNBT(nbt)
  }

  override def writeToNBT(nbt: NBTTagCompound) = {
    super.writeToNBT(nbt)
    computer.writeToNBT(nbt)
  }

  override def updateEntity() = computer.update()
}