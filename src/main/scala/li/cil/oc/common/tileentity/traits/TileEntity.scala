package li.cil.oc.common.tileentity.traits

import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.client.Sound
import li.cil.oc.common.SaveHandler
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.SideTracker
import net.minecraft.block.BlockState
import net.minecraft.nbt.CompoundNBT
import net.minecraft.network.NetworkManager
import net.minecraft.network.play.server.SUpdateTileEntityPacket
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

trait TileEntity extends net.minecraft.tileentity.TileEntity {
  private final val IsServerDataTag = Settings.namespace + "isServerData"

  def x: Int = getBlockPos.getX

  def y: Int = getBlockPos.getY

  def z: Int = getBlockPos.getZ

  def position = BlockPosition(x, y, z, getLevel)

  def isClient: Boolean = !isServer

  def isServer: Boolean = if (getLevel != null) !getLevel.isClientSide else SideTracker.isServer

  // ----------------------------------------------------------------------- //

  def updateEntity() {
    if (Settings.get.periodicallyForceLightUpdate && getLevel.getGameTime % 40 == 0 && getBlockState.getBlock.getLightValue(getLevel.getBlockState(getBlockPos), getLevel, getBlockPos) > 0) {
      getLevel.sendBlockUpdated(getBlockPos, getLevel.getBlockState(getBlockPos), getLevel.getBlockState(getBlockPos), 3)
    }
  }

  override def clearRemoved() {
    super.clearRemoved()
    initialize()
  }

  override def setRemoved() {
    super.setRemoved()
    dispose()
  }

  override def onChunkUnloaded() {
    super.onChunkUnloaded()
    try dispose() catch {
      case t: Throwable => OpenComputers.log.error("Failed properly disposing a tile entity, things may leak and or break.", t)
    }
  }

  protected def initialize() {
  }

  def dispose() {
    if (isClient) {
      // Note: chunk unload is handled by sound via event handler.
      Sound.stopLoop(this)
    }
  }

  // ----------------------------------------------------------------------- //

  def loadForServer(nbt: CompoundNBT) {}

  def saveForServer(nbt: CompoundNBT): Unit = {
    nbt.putBoolean(IsServerDataTag, true)
    super.save(nbt)
  }

  @OnlyIn(Dist.CLIENT)
  def loadForClient(nbt: CompoundNBT) {}

  def saveForClient(nbt: CompoundNBT): Unit = {
    nbt.putBoolean(IsServerDataTag, false)
  }

  // ----------------------------------------------------------------------- //

  override def load(state: BlockState, nbt: CompoundNBT): Unit = {
    super.load(state, nbt)
    if (isServer || nbt.getBoolean(IsServerDataTag)) {
      loadForServer(nbt)
    }
    else {
      loadForClient(nbt)
    }
  }

  override def save(nbt: CompoundNBT): CompoundNBT = {
    if (isServer) {
      saveForServer(nbt)
    }
    nbt
  }

  override def getUpdatePacket: SUpdateTileEntityPacket = {
    // Obfuscation workaround. If it works.
    val te = this.asInstanceOf[net.minecraft.tileentity.TileEntity]
    new SUpdateTileEntityPacket(te.getBlockPos, 0, te.getUpdateTag)
  }

  override def getUpdateTag: CompoundNBT = {
    val nbt = super.getUpdateTag

    // See comment on savingForClients variable.
    SaveHandler.savingForClients = true
    try {
      try saveForClient(nbt) catch {
        case e: Throwable => OpenComputers.log.warn("There was a problem writing a TileEntity description packet. Please report this if you see it!", e)
      }
    } finally {
      SaveHandler.savingForClients = false
    }

    nbt
  }

  override def onDataPacket(manager: NetworkManager, packet: SUpdateTileEntityPacket) {
    try loadForClient(packet.getTag) catch {
      case e: Throwable => OpenComputers.log.warn("There was a problem reading a TileEntity description packet. Please report this if you see it!", e)
    }
  }
}
