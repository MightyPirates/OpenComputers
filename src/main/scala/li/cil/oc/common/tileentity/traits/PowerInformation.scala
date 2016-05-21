package li.cil.oc.common.tileentity.traits

import li.cil.oc.Settings
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

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

  @SideOnly(Side.CLIENT)
  override def readFromNBTForClient(nbt: NBTTagCompound) {
    super.readFromNBTForClient(nbt)
    globalBuffer = nbt.getDouble(GlobalBufferTag)
    globalBufferSize = nbt.getDouble(GlobalBufferSizeTag)
  }

  override def writeToNBTForClient(nbt: NBTTagCompound) {
    super.writeToNBTForClient(nbt)
    lastSentRatio = if (globalBufferSize > 0) globalBuffer / globalBufferSize else 0
    nbt.setDouble(GlobalBufferTag, globalBuffer)
    nbt.setDouble(GlobalBufferSizeTag, globalBufferSize)
  }
}
