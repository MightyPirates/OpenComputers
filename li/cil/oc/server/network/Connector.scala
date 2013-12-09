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

  private var distributor: Option[PowerDistributor] = None

  // ----------------------------------------------------------------------- //

  def globalBuffer = distributor.fold(localBuffer)(_.globalBuffer)

  def globalBufferSize = distributor.fold(localBufferSize)(_.globalBufferSize)

  // ----------------------------------------------------------------------- //

  def changeBuffer(delta: Double): Double = {
    if (delta == 0) 0
    else if (Settings.get.ignorePower) {
      if (delta < 0) 0
      else /* if (delta > 0) */ delta
    }
    else {
      this.synchronized(distributor match {
        case Some(d) => d.synchronized(d.changeBuffer(change(delta)))
        case _ => change(delta)
      })
    }
  }

  private def change(delta: Double): Double = {
    if (localBufferSize <= 0) return delta
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
    if (localBuffer != oldBuffer) {
      this.synchronized(distributor match {
        case Some(d) => d.dirty = true
        case _ =>
      })
    }
    remaining
  }

  def tryChangeBuffer(delta: Double): Boolean = {
    if (delta == 0) true
    else if (Settings.get.ignorePower) delta < 0
    else {
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
  }

  def setLocalBufferSize(size: Double) {
    this.synchronized(distributor match {
      case Some(d) => d.synchronized {
        localBufferSize = math.max(size, 0)
        val surplus = math.max(localBuffer - localBufferSize, 0)
        localBuffer = math.min(localBuffer, localBufferSize)
        d.changeBuffer(surplus)
      }
      case _ =>
        localBufferSize = math.max(size, 0)
        localBuffer = math.min(localBuffer, localBufferSize)
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
    localBuffer = math.max(nbt.getDouble("buffer"), math.min(0, localBufferSize))
  }

  override def save(nbt: NBTTagCompound) {
    super.save(nbt)
    nbt.setDouble("buffer", localBuffer)
  }
}
