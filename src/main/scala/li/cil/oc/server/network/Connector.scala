package li.cil.oc.server.network

import li.cil.oc.Settings
import li.cil.oc.api.network
import li.cil.oc.api.network.{Node => ImmutableNode}
import net.minecraft.nbt.NBTTagCompound

trait Connector extends network.Connector with Node {
  var localBufferSize: Double

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
      this.synchronized(distributor match {
        case Some(d) => d.globalBuffer = math.max(0, math.min(d.globalBufferSize, d.globalBuffer - oldBuffer + localBuffer))
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
          if (localBuffer > localBufferSize) {
            d.changeBuffer(localBuffer - localBufferSize)
            localBuffer = localBufferSize
          }
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
        if (network != null) {
          if (localBufferSize <= 0 && size > 0) d.addConnector(this)
          else if (localBufferSize > 0 && size == 0) d.removeConnector(this)
          else d.globalBufferSize = math.max(d.globalBufferSize - localBufferSize + size, 0)
        }
        localBufferSize = math.max(size, 0)
        val surplus = math.max(localBuffer - localBufferSize, 0)
        changeBuffer(-surplus)
        d.changeBuffer(surplus)
      }
      case _ =>
        localBufferSize = math.max(size, 0)
        localBuffer = math.min(localBuffer, localBufferSize)
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
    localBuffer = nbt.getDouble("buffer")
  }

  override def save(nbt: NBTTagCompound) {
    super.save(nbt)
    nbt.setDouble("buffer", localBuffer)
  }
}
