package li.cil.oc.common.tileentity

import cpw.mods.fml.common.Optional
import li.cil.oc.Settings
import li.cil.oc.api.network.{Connector, SidedEnvironment}
import li.cil.oc.api.{Network, network}
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.ForgeDirection
import scala.math.ScalaNumber
import universalelectricity.api.UniversalClass
import universalelectricity.api.energy.{IEnergyContainer, IEnergyInterface}
import cpw.mods.fml.relauncher.{Side, SideOnly}

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
abstract class PowerAcceptor extends TileEntity with network.Environment with IEnergyInterface with IEnergyContainer {
  override def canConnect(direction: ForgeDirection, source: AnyRef) =
    (if (isClient) hasConnector(direction) else connector(direction).isDefined) &&
      direction != null && direction != ForgeDirection.UNKNOWN

  override def onReceiveEnergy(from: ForgeDirection, receive: Long, doReceive: Boolean) = connector(from) match {
    case Some(node) if !Settings.get.ignorePower =>
      val energy = fromUE(receive)
      if (doReceive) {
        val surplus = node.changeBuffer(energy)
        receive - toUE(surplus)
      }
      else {
        val space = node.globalBufferSize - node.globalBuffer
        math.min(receive, toUE(space))
      }
    case _ => 0
  }

  override def onExtractEnergy(from: ForgeDirection, extract: Long, doExtract: Boolean) = 0

  override def setEnergy(from: ForgeDirection, energy: Long) {}

  override def getEnergy(from: ForgeDirection) = connector(from) match {
    case Some(node) => toUE(node.globalBuffer)
    case _ => 0
  }

  override def getEnergyCapacity(from: ForgeDirection) = connector(from) match {
    case Some(node) => toUE(node.globalBufferSize)
    case _ => 0
  }

  protected def toUE(energy: Double) = (energy * Settings.ratioBC).toLong

  protected def fromUE(energy: Long) = energy / Settings.ratioBC

  @SideOnly(Side.CLIENT)
  protected def hasConnector(side: ForgeDirection) = false

  protected def connector(side: ForgeDirection): Option[Connector] = None
}

abstract class Environment extends PowerAcceptor {
  protected var addedToNetwork = false

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
    this match {
      case sidedEnvironment: SidedEnvironment => for (side <- ForgeDirection.VALID_DIRECTIONS) {
        Option(sidedEnvironment.sidedNode(side)).foreach(_.remove())
      }
      case _ =>
    }
  }

  override def invalidate() {
    super.invalidate()
    Option(node).foreach(_.remove)
    this match {
      case sidedEnvironment: SidedEnvironment => for (side <- ForgeDirection.VALID_DIRECTIONS) {
        Option(sidedEnvironment.sidedNode(side)).foreach(_.remove())
      }
      case _ =>
    }
  }

  // ----------------------------------------------------------------------- //

  override def readFromNBT(nbt: NBTTagCompound) {
    super.readFromNBT(nbt)
    if (node != null && node.host == this) {
      node.load(nbt.getCompoundTag(Settings.namespace + "node"))
    }
  }

  override def writeToNBT(nbt: NBTTagCompound) {
    super.writeToNBT(nbt)
    if (node != null && node.host == this) {
      nbt.setNewCompoundTag(Settings.namespace + "node", node.save)
    }
  }

  // ----------------------------------------------------------------------- //

  override def onMessage(message: network.Message) {}

  override def onConnect(node: network.Node) {}

  override def onDisconnect(node: network.Node) {}

  // ----------------------------------------------------------------------- //

  final protected def result(args: Any*): Array[AnyRef] = {
    def unwrap(arg: Any): AnyRef = arg match {
      case x: ScalaNumber => x.underlying
      case x => x.asInstanceOf[AnyRef]
    }
    Array(args map unwrap: _*)
  }
}
