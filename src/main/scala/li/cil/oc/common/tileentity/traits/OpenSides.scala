package li.cil.oc.common.tileentity.traits

import cpw.mods.fml.relauncher.{Side, SideOnly}
import li.cil.oc.Settings
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.ForgeDirection

/**
  * @author Vexatos
  */
trait OpenSides extends TileEntity {
  protected def SideCount = ForgeDirection.VALID_DIRECTIONS.length

  protected def defaultState: Boolean = false

  var openSides = Array.fill(SideCount)(defaultState)

  def compressSides = (ForgeDirection.VALID_DIRECTIONS, openSides).zipped.foldLeft(0)((acc, entry) => acc | (if (entry._2) entry._1.flag else 0)).toByte

  def uncompressSides(byte: Byte) = ForgeDirection.VALID_DIRECTIONS.map(d => (d.flag & byte) != 0)

  def isSideOpen(side: ForgeDirection) = side != ForgeDirection.UNKNOWN && openSides(side.ordinal())

  def setSideOpen(side: ForgeDirection, value: Boolean): Unit = if (side != ForgeDirection.UNKNOWN && openSides(side.ordinal()) != value) {
    openSides(side.ordinal()) = value
  }

  override def readFromNBTForServer(nbt: NBTTagCompound) {
    super.readFromNBTForServer(nbt)
    openSides = uncompressSides(nbt.getByte(Settings.namespace + "openSides"))
  }

  override def writeToNBTForServer(nbt: NBTTagCompound) {
    super.writeToNBTForServer(nbt)
    nbt.setByte(Settings.namespace + "openSides", compressSides)
  }

  @SideOnly(Side.CLIENT)
  override def readFromNBTForClient(nbt: NBTTagCompound) {
    super.readFromNBTForClient(nbt)
    openSides = uncompressSides(nbt.getByte(Settings.namespace + "openSides"))
  }

  override def writeToNBTForClient(nbt: NBTTagCompound) {
    super.writeToNBTForClient(nbt)
    nbt.setByte(Settings.namespace + "openSides", compressSides)
  }
}
