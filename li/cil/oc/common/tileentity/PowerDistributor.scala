package li.cil.oc.common.tileentity

import li.cil.oc.api
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

  // ----------------------------------------------------------------------- //

  override def updateEntity() {
    if (!worldObj.isRemote && connectors.exists(_.dirty)) {
      computeAverage()
      // Adjust buffer fill ratio for all buffers to average.
      connectors.foreach(c => c.buffer = c.bufferSize * average)
    }
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
      average = -1
    }
    else node match {
      case connector: Connector =>
        connectors -= connector
        computeAverage()
      case _ =>
    }
  }

  override def onConnect(node: Node) {
    super.onConnect(node)
    if (node == this.node) {
      for (node <- node.network.nodes) node match {
        case connector: Connector => connectors += connector
        case _ =>
      }
      computeAverage()
    }
    else node match {
      case connector: Connector => connectors += connector
      case _ =>
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

  private def computeAverage() {
    // Computer average fill ratio of all buffers.
    average = connectors.foldRight(0.0)((c, acc) => {
      c.dirty = false // clear dirty flag for all connectors
      acc + (c.buffer / c.bufferSize)
    }) / (connectors.size max 1) // avoid NaNs
    ServerPacketSender.sendPowerState(this)
  }
}
