package li.cil.oc.common.tileentity.traits

import cpw.mods.fml.relauncher.{Side, SideOnly}
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import net.minecraft.nbt.NBTTagCompound

trait PowerInformation extends TileEntity {
  private var lastSentRatio = -1.0

  def globalBuffer: Double

  def globalBuffer_=(value: Double)

  def globalBufferSize: Double

  def globalBufferSize_=(value: Double)

  protected def updatePowerInformation() {
    val ratio = if (globalBufferSize > 0) globalBuffer / globalBufferSize else 0
    if (lastSentRatio < 0 || math.abs(lastSentRatio - ratio) > (5.0 / 100.0)) {
      lastSentRatio = ratio
      ServerPacketSender.sendPowerState(this)
    }
  }

  @SideOnly(Side.CLIENT)
  override def readFromNBTForClient(nbt: NBTTagCompound) {
    super.readFromNBTForClient(nbt)
    globalBuffer = nbt.getDouble("globalBuffer")
    globalBufferSize = nbt.getDouble("globalBufferSize")
  }

  override def writeToNBTForClient(nbt: NBTTagCompound) {
    super.writeToNBTForClient(nbt)
    lastSentRatio = if (globalBufferSize > 0) globalBuffer / globalBufferSize else 0
    nbt.setDouble("globalBuffer", globalBuffer)
    nbt.setDouble("globalBufferSize", globalBufferSize)
  }
}
