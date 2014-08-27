package li.cil.oc.common.tileentity.traits

import java.util.logging.Level

import cpw.mods.fml.relauncher.{Side, SideOnly}
import li.cil.oc.OpenComputers
import li.cil.oc.client.Sound
import li.cil.oc.util.SideTracker
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.INetworkManager
import net.minecraft.network.packet.Packet132TileEntityData

trait TileEntity extends net.minecraft.tileentity.TileEntity {
  def world = getWorldObj

  def x = xCoord

  def y = yCoord

  def z = zCoord

  def block = getBlockType

  def isClient = SideTracker.isClient

  def isServer = SideTracker.isServer

  // ----------------------------------------------------------------------- //

  override def updateEntity() {
    super.updateEntity()
    if (world.getTotalWorldTime % 40 == 0 && block.getLightValue(world, x, y, z) > 0) {
      world.markBlockForUpdate(x, y, z)
    }
  }

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

  def dispose() {
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
    if (nbt.hasNoTags) null else new Packet132TileEntityData(x, y, z, -1, nbt)
  }

  override def onDataPacket(manager: INetworkManager, packet: Packet132TileEntityData) {
    try readFromNBTForClient(packet.data) catch {
      case e: Throwable => OpenComputers.log.log(Level.WARNING, "There was a problem reading a TileEntity description packet. Please report this if you see it!", e)
    }
  }
}
