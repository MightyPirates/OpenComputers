package li.cil.oc.common.tileentity.traits

import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.client.Sound
import li.cil.oc.common.SaveHandler
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.SideTracker
import net.minecraft.block.state.IBlockState
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.NetworkManager
import net.minecraft.network.play.server.S35PacketUpdateTileEntity
import net.minecraft.util.BlockPos
import net.minecraft.util.ITickable
import net.minecraft.world.World
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

// TODO only implement ticking interface where needed.
trait TileEntity extends net.minecraft.tileentity.TileEntity with ITickable {
  def world = getWorld

  def x = getPos.getX

  def y = getPos.getY

  def z = getPos.getZ

  def position = BlockPosition(x, y, z, world)

  def isClient = !isServer

  def isServer = if (world != null) !world.isRemote else SideTracker.isServer

  // ----------------------------------------------------------------------- //

  def canUpdate = true

  override def update(): Unit = {
    if (canUpdate) updateEntity()
  }

  def updateEntity() {
    if (Settings.get.periodicallyForceLightUpdate && world.getTotalWorldTime % 40 == 0 && getBlockType.getLightValue(world, getPos) > 0) {
      world.markBlockForUpdate(getPos)
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

  override def shouldRefresh(world: World, pos: BlockPos, oldState: IBlockState, newSate: IBlockState) = oldState.getBlock != newSate.getBlock

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
    if (nbt.hasNoTags) null else new S35PacketUpdateTileEntity(getPos, -1, nbt)
    } finally {
      SaveHandler.savingForClients = false
  }
  }

  override def onDataPacket(manager: NetworkManager, packet: S35PacketUpdateTileEntity) {
    try readFromNBTForClient(packet.getNbtCompound) catch {
      case e: Throwable => OpenComputers.log.warn("There was a problem reading a TileEntity description packet. Please report this if you see it!", e)
    }
  }
}
