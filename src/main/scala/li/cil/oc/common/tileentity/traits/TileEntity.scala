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
import net.minecraft.network.play.server.SPacketUpdateTileEntity
import net.minecraft.util.ITickable
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

// TODO only implement ticking interface where needed.
trait TileEntity extends net.minecraft.tileentity.TileEntity with ITickable {
  private final val IsServerDataTag = Settings.namespace + "isServerData"

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
    if (Settings.get.periodicallyForceLightUpdate && world.getTotalWorldTime % 40 == 0 && getBlockType.getLightValue(world.getBlockState(getPos), world, getPos) > 0) {
      world.notifyBlockUpdate(getPos, world.getBlockState(getPos), world.getBlockState(getPos), 3)
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

  def writeToNBTForServer(nbt: NBTTagCompound): Unit = {
    nbt.setBoolean(IsServerDataTag, true)
    super.writeToNBT(nbt)
  }

  @SideOnly(Side.CLIENT)
  def readFromNBTForClient(nbt: NBTTagCompound) {}

  def writeToNBTForClient(nbt: NBTTagCompound): Unit = {
    nbt.setBoolean(IsServerDataTag, false)
  }

  // ----------------------------------------------------------------------- //

  override def readFromNBT(nbt: NBTTagCompound): Unit = {
    if (isServer || nbt.getBoolean(IsServerDataTag)) {
      readFromNBTForServer(nbt)
    }
    else {
      readFromNBTForClient(nbt)
    }
  }

  override def writeToNBT(nbt: NBTTagCompound): NBTTagCompound = {
    if (isServer) {
      writeToNBTForServer(nbt)
    }
    nbt
  }

  override def getUpdatePacket: SPacketUpdateTileEntity = new SPacketUpdateTileEntity(getPos, getBlockMetadata, getUpdateTag)

  override def getUpdateTag: NBTTagCompound = {
    val nbt = super.getUpdateTag

    // See comment on savingForClients variable.
    SaveHandler.savingForClients = true
    try {
      try writeToNBTForClient(nbt) catch {
        case e: Throwable => OpenComputers.log.warn("There was a problem writing a TileEntity description packet. Please report this if you see it!", e)
      }
    } finally {
      SaveHandler.savingForClients = false
    }

    nbt
  }

  override def onDataPacket(manager: NetworkManager, packet: SPacketUpdateTileEntity) {
    try readFromNBTForClient(packet.getNbtCompound) catch {
      case e: Throwable => OpenComputers.log.warn("There was a problem reading a TileEntity description packet. Please report this if you see it!", e)
    }
  }
}
