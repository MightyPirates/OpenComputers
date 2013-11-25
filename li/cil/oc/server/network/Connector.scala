package li.cil.oc.server.network

import li.cil.oc.Settings
import li.cil.oc.api.network
import li.cil.oc.api.network.{Node => ImmutableNode}
import li.cil.oc.server.component.PowerDistributor
import li.cil.oc.util.Persistable
import net.minecraft.nbt.NBTTagCompound
import scala.collection.convert.WrapAsScala._

trait Connector extends Node with network.Connector with Persistable {
  val localBufferSize: Double

  var localBuffer = 0.0

  var dirty = true

  private var distributor: Option[PowerDistributor] = None

  // ----------------------------------------------------------------------- //

  def globalBuffer = distributor.fold(localBuffer)(_.globalBuffer)

  def globalBufferSize = distributor.fold(localBufferSize)(_.globalBufferSize)

  // ----------------------------------------------------------------------- //

  def changeBuffer(delta: Double) = if (delta != 0) {
    val remaining = this.synchronized {
      val oldBuffer = localBuffer
      localBuffer = localBuffer + delta
      val remaining = if (localBuffer < 0) {
        val remaining = localBuffer
        localBuffer = 0
        remaining
      }
      else if (localBuffer > localBufferSize) {
        val remaining = localBuffer - localBufferSize
        localBuffer = localBufferSize
        remaining
      }
      else 0
      dirty ||= (localBuffer != oldBuffer)
      remaining
    }
    distributor.fold(remaining == 0)(_.changeBuffer(remaining)) || Settings.get.ignorePower
  } else true

  // ----------------------------------------------------------------------- //

  override def onConnect(node: ImmutableNode) {
    if (node == this) findDistributor()
    else if (distributor.isEmpty) node.host match {
      case distributor: PowerDistributor =>
        this.distributor = Some(distributor)
      case _ =>
    }
    super.onConnect(node)
  }

  override def onDisconnect(node: ImmutableNode) {
    if (node != this && distributor.exists(_ == node)) findDistributor()
    super.onDisconnect(node)
  }

  private def findDistributor() = {
    distributor = reachableNodes.find(_.host.isInstanceOf[PowerDistributor]).fold(None: Option[PowerDistributor])(n => Some(n.host.asInstanceOf[PowerDistributor]))
  }

  // ----------------------------------------------------------------------- //

  override def load(nbt: NBTTagCompound) {
    super.load(nbt)
    localBuffer = nbt.getDouble("buffer") max 0 min localBufferSize
    dirty = true
  }

  override def save(nbt: NBTTagCompound) {
    super.save(nbt)
    nbt.setDouble("buffer", localBuffer)
  }
}
