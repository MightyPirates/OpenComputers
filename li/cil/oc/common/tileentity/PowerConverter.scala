package li.cil.oc.common.tileentity

import li.cil.oc.api.network._
import li.cil.oc.api.{Network, network}
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.{Settings, api}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.ForgeDirection
import universalelectricity.api.energy.{IEnergyContainer, IEnergyInterface}
import universalelectricity.api.{CompatibilityType, UniversalClass}

// Because @UniversalClass injects custom invalidate and validate methods for
// IC2 setup/teardown we have to use a base class and implement our own logic
// in a child class. This also means we can't use the Environment base class,
// since mixins are linked up at compile time, whereas UniversalClass injects
// its methods at runtime.
@UniversalClass
abstract class PowerConverterBase extends TileEntity with network.Environment with IEnergyInterface with IEnergyContainer {
  def node: Connector

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

class PowerConverter extends PowerConverterBase with Analyzable {
  val node = api.Network.newNode(this, Visibility.Network).
    withConnector(Settings.get.bufferConverter).
    create()

  protected var addedToNetwork = false

  // ----------------------------------------------------------------------- //

  def onAnalyze(player: EntityPlayer, side: Int, hitX: Float, hitY: Float, hitZ: Float) = null

  def canConnect(direction: ForgeDirection) = direction != null && direction != ForgeDirection.UNKNOWN

  // ----------------------------------------------------------------------- //

  override def updateEntity() {
    super.updateEntity()
    if (!addedToNetwork) {
      addedToNetwork = true
      Network.joinOrCreateNetwork(this)
    }
  }

  override def onChunkUnload() {
    super.onChunkUnload()
    Option(node).foreach(_.remove)
  }

  override def invalidate() {
    super.invalidate()
    Option(node).foreach(_.remove)
  }

  // ----------------------------------------------------------------------- //

  override def readFromNBT(nbt: NBTTagCompound) {
    super.readFromNBT(nbt)
    if (node != null) {
      node.load(nbt.getCompoundTag(Settings.namespace + "node"))
    }
  }

  override def writeToNBT(nbt: NBTTagCompound) {
    super.writeToNBT(nbt)
    if (node != null) {
      nbt.setNewCompoundTag(Settings.namespace + "node", node.save)
    }
  }

  // ----------------------------------------------------------------------- //

  def onMessage(message: network.Message) {}

  def onConnect(node: network.Node) {}

  def onDisconnect(node: network.Node) {}
}
