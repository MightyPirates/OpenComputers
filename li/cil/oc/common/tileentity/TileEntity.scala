package li.cil.oc.common.tileentity

import cpw.mods.fml.relauncher.{Side, SideOnly}
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.INetworkManager
import net.minecraft.network.packet.Packet132TileEntityData
import net.minecraft.tileentity.{TileEntity => MCTileEntity}

trait TileEntity extends MCTileEntity {
  def world = getWorldObj

  def x = xCoord

  def y = yCoord

  def z = zCoord

  def block = getBlockType

  lazy val isClient = world.isRemote

  lazy val isServer = !isClient

  // ----------------------------------------------------------------------- //

  override def getDescriptionPacket = {
    val nbt = new NBTTagCompound()
    writeToNBTForClient(nbt)
    if (nbt.hasNoTags) null else new Packet132TileEntityData(x, y, z, -1, nbt)
  }

  override def onDataPacket(manager: INetworkManager, packet: Packet132TileEntityData) {
    readFromNBTForClient(packet.data)
  }

  @SideOnly(Side.CLIENT)
  def readFromNBTForClient(nbt: NBTTagCompound) {}

  def writeToNBTForClient(nbt: NBTTagCompound) {}
}
