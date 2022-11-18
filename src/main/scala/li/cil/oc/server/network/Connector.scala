package li.cil.oc.server.network

import li.cil.oc.Settings
import li.cil.oc.api.network
import li.cil.oc.api.network.{Node => ImmutableNode}
import li.cil.oc.common.item.data.NodeData
import net.minecraft.nbt.CompoundNBT

trait Connector extends network.Connector with Node {
  var localBufferSize = 0.0

  var localBuffer = 0.0

  var distributor: Option[Distributor] = None

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
      distributor match {
        case Some(d) => d.globalBuffer = math.max(0, math.min(d.globalBufferSize, d.globalBuffer - oldBuffer + localBuffer))
        case _ =>
      }
    }
    remaining
  }

  def tryChangeBuffer(delta: Double): Boolean = {
    if (delta == 0) true
    else if (Settings.get.ignorePower) delta < 0
    else {
      this.synchronized(distributor match {
        case Some(d) => d.synchronized {
          if (localBuffer > localBufferSize) {
            d.changeBuffer(localBuffer - localBufferSize)
            localBuffer = localBufferSize
          }
          val newGlobalBuffer = globalBuffer + delta
          (delta > 0 || newGlobalBuffer >= 0) && (delta < 0 || newGlobalBuffer <= globalBufferSize) && d.changeBuffer(delta) == 0
        }
        case _ =>
          val newLocalBuffer = localBuffer + delta
          if ((delta < 0 && newLocalBuffer < 0) || (delta > 0 && newLocalBuffer > localBufferSize)) {
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
    val clampedSize = math.max(size, 0)
    this.synchronized(distributor match {
      case Some(d) => d.synchronized {
        val oldSize = localBufferSize
        // Must apply new size before trying to register with distributor, else
        // we get ignored if our size is zero.
        localBufferSize = clampedSize
        if (network != null) {
          if (oldSize <= 0 && clampedSize > 0) d.addConnector(this)
          else if (oldSize > 0 && clampedSize == 0) d.removeConnector(this)
          else d.globalBufferSize = math.max(d.globalBufferSize - oldSize + clampedSize, 0)
        }
        val surplus = math.max(localBuffer - clampedSize, 0)
        changeBuffer(-surplus)
        d.changeBuffer(surplus)
      }
      case _ =>
        localBufferSize = clampedSize
        localBuffer = math.min(localBuffer, localBufferSize)
    })
  }

  // ----------------------------------------------------------------------- //

  override def onDisconnect(node: ImmutableNode) {
    super.onDisconnect(node)
    if (node == this) {
      this.synchronized(this.distributor = None)
    }
  }

  // ----------------------------------------------------------------------- //

  override def loadData(nbt: CompoundNBT) {
    super.loadData(nbt)
    localBuffer = nbt.getDouble(NodeData.BufferTag)
  }

  override def saveData(nbt: CompoundNBT) {
    super.saveData(nbt)
    nbt.putDouble(NodeData.BufferTag, math.min(localBuffer, localBufferSize))
  }
}
