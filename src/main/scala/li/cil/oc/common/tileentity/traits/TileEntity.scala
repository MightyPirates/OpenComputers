package li.cil.oc.common.tileentity.traits

import cpw.mods.fml.relauncher.{Side, SideOnly}
import java.util.logging.Level
import li.cil.oc.OpenComputers
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.INetworkManager
import net.minecraft.network.packet.Packet132TileEntityData

trait TileEntity extends net.minecraft.tileentity.TileEntity {
  def world = getWorldObj

  def x = xCoord

  def y = yCoord

  def z = zCoord

  def block = getBlockType

  lazy val isClient = world.isRemote

  lazy val isServer = !isClient

  @SideOnly(Side.CLIENT)
  def readFromNBTForClient(nbt: NBTTagCompound) {}

  def writeToNBTForClient(nbt: NBTTagCompound) {}

  // ----------------------------------------------------------------------- //

  override def getDescriptionPacket = {
    val nbt = new NBTTagCompound()
    try writeToNBTForClient(nbt) catch {
      case e: Throwable => OpenComputers.log.log(Level.WARNING, "There was a problem writing a TileEntity description packet. Please report this if you see it!", e)
    }
    if (nbt.hasNoTags) null else new Packet132TileEntityData(x, y, z, -1, nbt)
  }

  override def onDataPacket(manager: INetworkManager, packet: Packet132TileEntityData) {
    try readFromNBTForClient(packet.data) catch {
      case e: Throwable => OpenComputers.log.log(Level.WARNING, "There was a problem reading a TileEntity description packet. Please report this if you see it!", e)
    }
  }
}
