package li.cil.oc.common.tileentity.traits

import cpw.mods.fml.relauncher.{Side, SideOnly}
import java.util.logging.Level
import li.cil.oc.client.Sound
import li.cil.oc.OpenComputers
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.NetworkManager
import net.minecraft.network.play.server.S35PacketUpdateTileEntity
import cpw.mods.fml.common.FMLCommonHandler

trait TileEntity extends net.minecraft.tileentity.TileEntity {
  def world = getWorldObj

  def x = xCoord

  def y = yCoord

  def z = zCoord

  def block = getBlockType

  val isClient = FMLCommonHandler.instance.getEffectiveSide.isClient

  val isServer = FMLCommonHandler.instance.getEffectiveSide.isServer

  // ----------------------------------------------------------------------- //

  override def validate() {
    super.validate()
    initialize()
  }

  override def invalidate() {
    super.invalidate()
    dispose()
  }

  override def onChunkUnload() {
    super.onChunkUnload()
    dispose()
  }

  protected def initialize() {}

  protected def dispose() {
    if (isClient) {
      // Note: chunk unload is handled by sound via event handler.
      Sound.stopLoop(this)
    }
  }

  // ----------------------------------------------------------------------- //

  @SideOnly(Side.CLIENT)
  def readFromNBTForClient(nbt: NBTTagCompound) {}

  def writeToNBTForClient(nbt: NBTTagCompound) {}

  // ----------------------------------------------------------------------- //

  override def getDescriptionPacket = {
    val nbt = new NBTTagCompound()
    try writeToNBTForClient(nbt) catch {
      case e: Throwable => OpenComputers.log.log(Level.WARNING, "There was a problem writing a TileEntity description packet. Please report this if you see it!", e)
    }
    if (nbt.hasNoTags) null else new S35PacketUpdateTileEntity(x, y, z, -1, nbt)
  }

  override def onDataPacket(manager: NetworkManager, packet: S35PacketUpdateTileEntity) {
    try readFromNBTForClient(packet.func_148857_g()) catch {
      case e: Throwable => OpenComputers.log.log(Level.WARNING, "There was a problem reading a TileEntity description packet. Please report this if you see it!", e)
    }
  }
}
