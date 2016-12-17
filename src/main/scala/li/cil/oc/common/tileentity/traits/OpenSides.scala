package li.cil.oc.common.tileentity.traits

import li.cil.oc.Settings
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

/**
  * @author Vexatos
  */
trait OpenSides extends TileEntity {
  protected def SideCount = EnumFacing.VALUES.length

  protected def defaultState: Boolean = false

  var openSides = Array.fill(SideCount)(defaultState)

  def compressSides = (EnumFacing.values(), openSides).zipped.foldLeft(0)((acc, entry) => acc | (if (entry._2) 1 << entry._1.ordinal() else 0)).toByte

  def uncompressSides(byte: Byte) = EnumFacing.values().map(d => ((1 << d.ordinal()) & byte) != 0)

  def isSideOpen(side: EnumFacing) = side != null && openSides(side.ordinal())

  def setSideOpen(side: EnumFacing, value: Boolean): Unit = if (side != null && openSides(side.ordinal()) != value) {
    openSides(side.ordinal()) = value
  }

  override def readFromNBTForServer(nbt: NBTTagCompound) {
    super.readFromNBTForServer(nbt)
    if (nbt.hasKey(Settings.namespace + "openSides"))
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
