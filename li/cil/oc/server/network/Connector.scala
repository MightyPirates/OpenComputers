package li.cil.oc.server.network

import li.cil.oc.Settings
import li.cil.oc.api.network
import li.cil.oc.api.network.{Node => ImmutableNode}
import li.cil.oc.server.component.PowerDistributor
import li.cil.oc.util.Persistable
import net.minecraft.nbt.NBTTagCompound
import scala.collection.convert.WrapAsScala._

trait Connector extends Node with network.Connector with Persistable {
  var localBufferSize: Double

  var localBuffer = 0.0

  var dirty = true

  private var distributor: Option[PowerDistributor] = None

  // ----------------------------------------------------------------------- //

  def globalBuffer = distributor.fold(localBuffer)(_.globalBuffer)

  def globalBufferSize = distributor.fold(localBufferSize)(_.globalBufferSize)

  // ----------------------------------------------------------------------- //

  def changeBuffer(delta: Double): Double = {
    if (delta == 0) {
      return 0
    }
    if (Settings.get.ignorePower) {
      if (delta < 0) {
        return 0
      }
      else /* if (delta > 0) */ {
        return delta
      }
    }
    def change() = {
      val oldBuffer = localBuffer
      localBuffer += delta
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
    this.synchronized(distributor match {
      case Some(d) => d.synchronized(d.changeBuffer(change()))
      case _ => change()
    })
  }

  def tryChangeBuffer(delta: Double): Boolean = {
    if (delta == 0) {
      return true
    }
    if (Settings.get.ignorePower) {
      if (delta < 0) {
        return true
      }
      else /* if (delta > 0) */ {
        return false
      }
    }
    this.synchronized(distributor match {
      case Some(d) => d.synchronized {
        val newGlobalBuffer = globalBuffer + delta
        newGlobalBuffer >= 0 && newGlobalBuffer <= globalBufferSize && d.changeBuffer(delta) == 0
      }
      case _ =>
        val newLocalBuffer = localBuffer + delta
        if (newLocalBuffer < 0 || newLocalBuffer > localBufferSize) {
          false
        }
        else {
          localBuffer = newLocalBuffer
          true
        }
    })
  }

  def setLocalBufferSize(size: Double) {
    this.synchronized(distributor match {
      case Some(d) => d.synchronized {
        localBufferSize = size max 0
        val surplus = (localBuffer - localBufferSize) max 0
        localBuffer = localBuffer min localBufferSize
        d.changeBuffer(surplus)
      }
      case _ =>
        localBufferSize = size max 0
        localBuffer = localBuffer min localBufferSize
    })
  }

  // ----------------------------------------------------------------------- //

  override def onConnect(node: ImmutableNode) {
    if (node == this) {
      findDistributor()
    }
    else if (distributor.isEmpty) {
      node.host match {
        case d: PowerDistributor => this.synchronized(distributor = Some(d))
        case _ =>
      }
    }
    super.onConnect(node)
  }

  override def onDisconnect(node: ImmutableNode) {
    if (node == this) this.synchronized {
      setLocalBufferSize(0)
      distributor = None
    }
    else if (distributor.exists(_ == node.host)) {
      findDistributor()
    }
    super.onDisconnect(node)
  }

  private def findDistributor() = {
    this.synchronized(distributor = reachableNodes.find(_.host.isInstanceOf[PowerDistributor]).fold(None: Option[PowerDistributor])(n => Some(n.host.asInstanceOf[PowerDistributor])))
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
