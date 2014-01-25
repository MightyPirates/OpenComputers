package li.cil.oc.common.tileentity

import li.cil.oc.api.network._
import li.cil.oc.{Settings, api}
import net.minecraft.entity.player.EntityPlayer
import net.minecraftforge.common.ForgeDirection
import universalelectricity.api.energy.{IEnergyContainer, IEnergyInterface}
import universalelectricity.api.{CompatibilityType, UniversalClass}

@UniversalClass
class PowerConverter extends Environment with Analyzable with IEnergyInterface with IEnergyContainer {
  val node = api.Network.newNode(this, Visibility.Network).
    withConnector(Settings.get.bufferConverter).
    create()

  def onAnalyze(player: EntityPlayer, side: Int, hitX: Float, hitY: Float, hitZ: Float) = null

  def canConnect(direction: ForgeDirection) = direction != null && direction != ForgeDirection.UNKNOWN

  def onReceiveEnergy(from: ForgeDirection, receive: Long, doReceive: Boolean) = {
    if (!Settings.get.ignorePower && node != null) {
      val energy = fromUE(receive)
      if (doReceive) {
        val surplus = node.changeBuffer(energy)
        receive - toUE(surplus)
      }
      else {
        val space = node.globalBufferSize - node.globalBuffer
        math.min(receive, toUE(space))
      }
    }
    else 0
  }

  def onExtractEnergy(from: ForgeDirection, extract: Long, doExtract: Boolean) = 0

  def setEnergy(from: ForgeDirection, energy: Long) {}

  def getEnergy(from: ForgeDirection) = if (node != null) toUE(node.globalBuffer) else 0

  def getEnergyCapacity(from: ForgeDirection) = if (node != null) toUE(node.globalBufferSize) else Long.MaxValue

  protected def toUE(energy: Double) = (energy * CompatibilityType.BUILDCRAFT.reciprocal_ratio).toLong

  protected def fromUE(energy: Long) = energy * CompatibilityType.BUILDCRAFT.ratio
}
