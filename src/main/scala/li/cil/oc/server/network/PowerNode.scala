package li.cil.oc.server.network

import li.cil.oc.Settings
import li.cil.oc.api.network
import li.cil.oc.api.network.{EnergyNode, Node => ImmutableNode}
import li.cil.oc.common.item.data.NodeData
import net.minecraft.nbt.NBTTagCompound

trait PowerNode extends PowerNode with Node {
  var getLocalBufferSize = 0.0

  var getLocalBuffer = 0.0

  var distributor: Option[Distributor] = None

  // ----------------------------------------------------------------------- //

  def getGlobalBuffer = distributor.fold(getLocalBuffer)(_.globalBuffer)

  def getGlobalBufferSize = distributor.fold(getLocalBufferSize)(_.globalBufferSize)

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
    if (getLocalBufferSize <= 0) return delta
    val oldBuffer = getLocalBuffer
    getLocalBuffer += delta
    val remaining = if (getLocalBuffer < 0) {
      val remaining = getLocalBuffer
      getLocalBuffer = 0
      remaining
    }
    else if (getLocalBuffer > getLocalBufferSize) {
      val remaining = getLocalBuffer - getLocalBufferSize
      getLocalBuffer = getLocalBufferSize
      remaining
    }
    else 0
    if (getLocalBuffer != oldBuffer) {
      distributor match {
        case Some(d) => d.globalBuffer = math.max(0, math.min(d.globalBufferSize, d.globalBuffer - oldBuffer + getLocalBuffer))
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
          if (getLocalBuffer > getLocalBufferSize) {
            d.changeBuffer(getLocalBuffer - getLocalBufferSize)
            getLocalBuffer = getLocalBufferSize
          }
          val newGlobalBuffer = getGlobalBuffer + delta
          (delta > 0 || newGlobalBuffer >= 0) && (delta < 0 || newGlobalBuffer <= getGlobalBufferSize) && d.changeBuffer(delta) == 0
        }
        case _ =>
          val newLocalBuffer = getLocalBuffer + delta
          if ((delta < 0 && newLocalBuffer < 0) || (delta > 0 && newLocalBuffer > getLocalBufferSize)) {
            false
          }
          else {
            getLocalBuffer = newLocalBuffer
            true
          }
      })
    }
  }

  def setLocalBufferSize(size: Double) {
    val clampedSize = math.max(size, 0)
    this.synchronized(distributor match {
      case Some(d) => d.synchronized {
        val oldSize = getLocalBufferSize
        // Must apply new size before trying to register with distributor, else
        // we get ignored if our size is zero.
        getLocalBufferSize = clampedSize
        if (getNetwork != null) {
          if (oldSize <= 0 && clampedSize > 0) d.addConnector(this)
          else if (oldSize > 0 && clampedSize == 0) d.removeConnector(this)
          else d.globalBufferSize = math.max(d.globalBufferSize - oldSize + clampedSize, 0)
        }
        val surplus = math.max(getLocalBuffer - clampedSize, 0)
        changeBuffer(-surplus)
        d.changeBuffer(surplus)
      }
      case _ =>
        getLocalBufferSize = clampedSize
        getLocalBuffer = math.min(getLocalBuffer, getLocalBufferSize)
    })
  }

  // ----------------------------------------------------------------------- //

  override def onDisconnect(node: ImmutableNode) {
    super.onDisconnect(node)
    if (node == this) {
      this.synchronized(distributor = None)
    }
  }

  // ----------------------------------------------------------------------- //

  override def load(nbt: NBTTagCompound) {
    super.load(nbt)
    getLocalBuffer = nbt.getDouble(NodeData.BufferTag)
  }

  override def save(nbt: NBTTagCompound) {
    super.save(nbt)
    nbt.setDouble(NodeData.BufferTag, math.min(getLocalBuffer, getLocalBufferSize))
  }
}
