package li.cil.oc.common.tileentity

import li.cil.oc.api
import li.cil.oc.api.Network
import li.cil.oc.api.network._
import li.cil.oc.client.{PacketSender => ClientPacketSender}
import li.cil.oc.server.network.Connector
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import net.minecraft.nbt.NBTTagCompound
import scala.collection.convert.WrapAsScala._
import scala.collection.mutable

class PowerDistributor extends Rotatable with Environment {
  val node = api.Network.newNode(this, Visibility.Network).create()

  val connectors = mutable.Set.empty[Connector]

  var average = 0.0

  private var lastSentAverage = 0.0

  private val distributors = mutable.Set.empty[PowerDistributor]

  // ----------------------------------------------------------------------- //

  override def updateEntity() {
    super.updateEntity()
    if (node != null && node.network == null) {
      Network.joinOrCreateNetwork(worldObj, xCoord, yCoord, zCoord)
    }
    if (!worldObj.isRemote && connectors.exists(_.dirty))
      balance()
  }

  override def validate() {
    super.validate()
    if (worldObj.isRemote) ClientPacketSender.sendPowerStateRequest(this)
  }

  // ----------------------------------------------------------------------- //

  override def onDisconnect(node: Node) {
    super.onDisconnect(node)
    if (node == this.node) {
      connectors.clear()
      distributors.clear()
      average = -1
    }
    else node match {
      case connector: Connector =>
        connectors -= connector
        balance()
      case _ => node.host match {
        case distributor: PowerDistributor => distributors -= distributor
        case _ =>
      }
    }
  }

  override def onConnect(node: Node) {
    super.onConnect(node)
    if (node == this.node) {
      for (node <- node.network.nodes) node match {
        case connector: Connector => connectors += connector
        case _ => node.host match {
          case distributor: PowerDistributor => distributors += distributor
          case _ =>
        }
      }
      balance()
    }
    else node match {
      case connector: Connector => connectors += connector
      case _ => node.host match {
        case distributor: PowerDistributor => distributors += distributor
        case _ =>
      }
    }
  }

  // ----------------------------------------------------------------------- //

  override def readFromNBT(nbt: NBTTagCompound) {
    super[Rotatable].readFromNBT(nbt)
    node.load(nbt)
  }

  override def writeToNBT(nbt: NBTTagCompound) {
    super[Rotatable].writeToNBT(nbt)
    node.save(nbt)
  }

  // ----------------------------------------------------------------------- //

  private def balance() {
    // Computer average fill ratio of all buffers.
    val (minRelativeBuffer, maxRelativeBuffer, sumBuffer, sumBufferSize) =
      connectors.foldRight((1.0, 0.0, 0.0, 0.0))((c, acc) => {
        c.dirty = false // clear dirty flag for all connectors
        (acc._1 min (c.buffer / c.bufferSize), acc._2 max (c.buffer / c.bufferSize),
          acc._3 + c.buffer, acc._4 + c.bufferSize)
      })
    average = if (sumBufferSize > 0) sumBuffer / sumBufferSize else 0
    if ((lastSentAverage - average).abs > 0.05) {
      lastSentAverage = average
      for (distributor <- distributors) {
        distributor.average = average
        distributor.lastSentAverage = lastSentAverage
        ServerPacketSender.sendPowerState(distributor)
      }
    }
    if (maxRelativeBuffer - minRelativeBuffer > 10e-4) {
      // Adjust buffer fill ratio for all buffers to average.
      connectors.foreach(c => c.buffer = c.bufferSize * average)
    }
  }
}
