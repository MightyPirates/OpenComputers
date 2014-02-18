package li.cil.oc.common.tileentity

import cpw.mods.fml.common.Optional
import li.cil.oc.api.network._
import li.cil.oc.api.{Network, network}
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.{Settings, api}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.ForgeDirection
import universalelectricity.api.UniversalClass
import universalelectricity.api.energy.{IEnergyContainer, IEnergyInterface}

// Because @UniversalClass injects custom invalidate and validate methods for
// IC2 setup/teardown we have to use a base class and implement our own logic
// in a child class. This also means we can't use the Environment base class,
// since mixins are linked up at compile time, whereas UniversalClass injects
// its methods at runtime.
@UniversalClass
@Optional.InterfaceList(Array(
  new Optional.Interface(iface = "universalelectricity.api.energy.IEnergyInterface", modid = "UniversalElectricity"),
  new Optional.Interface(iface = "universalelectricity.api.energy.IEnergyContainer", modid = "UniversalElectricity")
))
abstract class PowerConverterBase extends TileEntity with network.Environment with IEnergyInterface with IEnergyContainer {
  override def node: Connector

  override def canConnect(direction: ForgeDirection, source: AnyRef) = direction != null && direction != ForgeDirection.UNKNOWN

  override def onReceiveEnergy(from: ForgeDirection, receive: Long, doReceive: Boolean) = {
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

  override def onExtractEnergy(from: ForgeDirection, extract: Long, doExtract: Boolean) = 0

  override def setEnergy(from: ForgeDirection, energy: Long) {}

  override def getEnergy(from: ForgeDirection) = if (node != null) toUE(node.globalBuffer) else 0

  override def getEnergyCapacity(from: ForgeDirection) = if (node != null) toUE(node.globalBufferSize) else Long.MaxValue

  protected def toUE(energy: Double) = (energy * Settings.ratioBC).toLong

  protected def fromUE(energy: Long) = energy / Settings.ratioBC
}

class PowerConverter extends PowerConverterBase with Analyzable {
  val node = api.Network.newNode(this, Visibility.Network).
    withConnector(Settings.get.bufferConverter).
    create()

  protected var addedToNetwork = false

  // ----------------------------------------------------------------------- //

  override def onAnalyze(player: EntityPlayer, side: Int, hitX: Float, hitY: Float, hitZ: Float) = null

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

  override def onMessage(message: network.Message) {}

  override def onConnect(node: network.Node) {}

  override def onDisconnect(node: network.Node) {}
}
