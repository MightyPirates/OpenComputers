package li.cil.oc.common.tileentity.traits

import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.client.Sound
import li.cil.oc.common.SaveHandler
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.SideTracker
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.NetworkManager
import net.minecraft.network.play.server.S35PacketUpdateTileEntity

trait TileEntity extends net.minecraft.tileentity.TileEntity {
  def world = getWorldObj

  def x = xCoord

  def y = yCoord

  def z = zCoord

  def position = BlockPosition(x, y, z, world)

  def block = getBlockType

  def isClient = !isServer

  def isServer = if (world != null) !world.isRemote else SideTracker.isServer

  // ----------------------------------------------------------------------- //

  override def updateEntity() {
    super.updateEntity()
    if (Settings.get.periodicallyForceLightUpdate && world.getTotalWorldTime % 40 == 0 && block.getLightValue(world, x, y, z) > 0) {
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

  def readFromNBTForServer(nbt: NBTTagCompound): Unit = super.readFromNBT(nbt)

  def writeToNBTForServer(nbt: NBTTagCompound): Unit = super.writeToNBT(nbt)

  @SideOnly(Side.CLIENT)
  def readFromNBTForClient(nbt: NBTTagCompound) {}

  def writeToNBTForClient(nbt: NBTTagCompound) {}

  // ----------------------------------------------------------------------- //

  override def readFromNBT(nbt: NBTTagCompound): Unit = {
    if (isServer) {
      readFromNBTForServer(nbt)
    }
  }

  override def writeToNBT(nbt: NBTTagCompound): Unit = {
    if (isServer) {
      writeToNBTForServer(nbt)
    }
  }

  override def getDescriptionPacket = {
    val nbt = new NBTTagCompound()

    // See comment on savingForClients variable.
    SaveHandler.savingForClients = true
    try {
      try writeToNBTForClient(nbt) catch {
        case e: Throwable => OpenComputers.log.warn("There was a problem writing a TileEntity description packet. Please report this if you see it!", e)
      }
      if (nbt.hasNoTags) null else new S35PacketUpdateTileEntity(x, y, z, -1, nbt)
    } finally {
      SaveHandler.savingForClients = false
    }
  }

  override def onDataPacket(manager: NetworkManager, packet: S35PacketUpdateTileEntity) {
    try readFromNBTForClient(packet.func_148857_g()) catch {
      case e: Throwable => OpenComputers.log.warn("There was a problem reading a TileEntity description packet. Please report this if you see it!", e)
    }
  }
}
