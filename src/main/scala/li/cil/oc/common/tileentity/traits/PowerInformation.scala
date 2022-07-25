package li.cil.oc.common.tileentity.traits

import li.cil.oc.Settings
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import net.minecraft.nbt.CompoundNBT
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

trait PowerInformation extends TileEntity {
  private var lastSentRatio = -1.0

  private var ticksUntilSync = 0

  def globalBuffer: Double

  def globalBuffer_=(value: Double): Unit

  def globalBufferSize: Double

  def globalBufferSize_=(value: Double): Unit

  protected def updatePowerInformation() {
    val ratio = if (globalBufferSize > 0) globalBuffer / globalBufferSize else 0
    if (shouldSync(ratio) || hasChangedSignificantly(ratio)) {
      lastSentRatio = ratio
      ServerPacketSender.sendPowerState(this)
    }
  }

  private def hasChangedSignificantly(ratio: Double) = lastSentRatio < 0 || math.abs(lastSentRatio - ratio) > (5.0 / 100.0)

  private def shouldSync(ratio: Double) = {
    ticksUntilSync -= 1
    if (ticksUntilSync <= 0) {
      ticksUntilSync = (100 / Settings.get.tickFrequency).toInt max 1
      lastSentRatio != ratio
    }
    else false
  }

  // ----------------------------------------------------------------------- //

  private final val GlobalBufferTag = Settings.namespace + "globalBuffer"
  private final val GlobalBufferSizeTag = Settings.namespace + "globalBufferSize"

  @OnlyIn(Dist.CLIENT)
  override def loadForClient(nbt: CompoundNBT) {
    super.loadForClient(nbt)
    globalBuffer = nbt.getDouble(GlobalBufferTag)
    globalBufferSize = nbt.getDouble(GlobalBufferSizeTag)
  }

  override def saveForClient(nbt: CompoundNBT) {
    super.saveForClient(nbt)
    lastSentRatio = if (globalBufferSize > 0) globalBuffer / globalBufferSize else 0
    nbt.putDouble(GlobalBufferTag, globalBuffer)
    nbt.putDouble(GlobalBufferSizeTag, globalBufferSize)
  }
}
