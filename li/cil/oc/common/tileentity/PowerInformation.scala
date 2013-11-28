package li.cil.oc.common.tileentity

import cpw.mods.fml.relauncher.{Side, SideOnly}
import net.minecraft.nbt.NBTTagCompound

trait PowerInformation extends TileEntity {
  def globalBuffer: Double

  def globalBuffer_=(value: Double)

  def globalBufferSize: Double

  def globalBufferSize_=(value: Double)

  @SideOnly(Side.CLIENT)
  override def readFromNBTForClient(nbt: NBTTagCompound) {
    super.readFromNBTForClient(nbt)
    globalBuffer = nbt.getDouble("globalBuffer")
    globalBufferSize = nbt.getDouble("globalBufferSize")
  }

  override def writeToNBTForClient(nbt: NBTTagCompound) {
    super.writeToNBTForClient(nbt)
    nbt.setDouble("globalBuffer", globalBuffer)
    nbt.setDouble("globalBufferSize", globalBufferSize)
  }
}
