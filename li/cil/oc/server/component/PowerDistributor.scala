package li.cil.oc.server.component

import li.cil.oc.api.network._
import li.cil.oc.common.tileentity.PowerInformation
import li.cil.oc.server.network.Connector
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import li.cil.oc.{Settings, api}
import scala.collection.convert.WrapAsScala._
import scala.collection.mutable

class PowerDistributor(val owner: PowerInformation) extends ManagedComponent {

  val node = api.Network.newNode(this, Visibility.Network).
    withComponent("power", Visibility.Network).
    create()

  var globalBuffer = 0.0

  var globalBufferSize = 0.0

  var dirty = true

  private var lastSentRatio = 0.0

  private val buffers = mutable.ArrayBuffer.empty[Connector]

  private val distributors = mutable.ArrayBuffer.empty[PowerDistributor]

  // ----------------------------------------------------------------------- //

  @LuaCallback(value = "buffer", direct = true)
  def buffer(context: Context, args: Arguments): Array[AnyRef] = result(globalBuffer)

  @LuaCallback(value = "bufferSize", direct = true)
  def bufferSize(context: Context, args: Arguments): Array[AnyRef] = result(globalBufferSize)

  // ----------------------------------------------------------------------- //

  def changeBuffer(delta: Double): Double = {
    if (delta == 0) 0
    else if (Settings.get.ignorePower) {
      if (delta < 0) 0
      else /* if (delta > 0) */ delta
    }
    else this.synchronized {
      val oldBuffer = globalBuffer
      globalBuffer = math.min(math.max(globalBuffer + delta, 0), globalBufferSize)
      if (globalBuffer == oldBuffer) {
        return delta
      }
      dirty = true
      if (delta < 0) {
        var remaining = -delta
        for (connector <- buffers if remaining > 0 && connector.localBufferSize > 0) {
          if (connector.localBuffer > 0) {
            if (connector.localBuffer < remaining) {
              remaining -= connector.localBuffer
              connector.localBuffer = 0
            }
            else {
              connector.localBuffer -= remaining
              remaining = 0
            }
          }
        }
        remaining
      }
      else /* if (delta > 0) */ {
        var remaining = delta
        for (connector <- buffers if remaining > 0 && connector.localBufferSize > 0) {
          if (connector.localBuffer < connector.localBufferSize) {
            val space = connector.localBufferSize - connector.localBuffer
            if (space < remaining) {
              remaining -= space
              connector.localBuffer = connector.localBufferSize
            }
            else {
              connector.localBuffer += remaining
              remaining = 0
            }
          }
        }
        remaining
      }
    }
  }

  // ----------------------------------------------------------------------- //

  override val canUpdate = true

  override def update() {
    if (dirty && owner.world.getWorldTime % Settings.get.tickFrequency == 0 && node != null) {
      updateCachedValues()
    }
  }

  // ----------------------------------------------------------------------- //

  override def onConnect(node: Node) {
    super.onConnect(node)
    if (node == this.node) {
      for (node <- node.reachableNodes) node match {
        case connector: Connector if connector.localBufferSize > 0 => this.synchronized {
          assert(!buffers.contains(connector))
          buffers += connector
          globalBuffer += connector.localBuffer
          globalBufferSize += connector.localBufferSize
        }
        case _ => node.host match {
          case distributor: PowerDistributor if distributor.node.canBeSeenFrom(this.node) =>
            assert(!distributors.contains(distributor))
            distributors += distributor
          case _ =>
        }
      }
      assert(!distributors.contains(this))
      distributors += this
      dirty = true
    }
    else node match {
      case connector: Connector if connector.localBufferSize > 0 => this.synchronized {
        assert(!buffers.contains(connector))
        buffers += connector
        globalBuffer += connector.localBuffer
        globalBufferSize += connector.localBufferSize
        dirty = true
      }
      case _ => node.host match {
        case distributor: PowerDistributor if distributor.node.canBeSeenFrom(this.node) =>
          assert(!distributors.contains(distributor))
          distributors += distributor
        case _ =>
      }
    }
  }

  override def onDisconnect(node: Node) {
    super.onDisconnect(node)
    if (node == this.node) this.synchronized {
      buffers.clear()
      distributors.clear()
      globalBuffer = 0
      globalBufferSize = 0
    }
    else node match {
      case connector: Connector => this.synchronized {
        buffers -= connector
        globalBuffer -= connector.localBuffer
        globalBufferSize -= connector.localBufferSize
        dirty = true
      }
      case _ => node.host match {
        case distributor: PowerDistributor => distributors -= distributor
        case _ =>
      }
    }
  }

  // ----------------------------------------------------------------------- //

  def updateCachedValues() {
    // Computer average fill ratio of all buffers.
    var sumBuffer, sumBufferSize = 0.0
    for (buffer <- buffers if buffer.localBufferSize > 0) {
      sumBuffer += buffer.localBuffer
      sumBufferSize += buffer.localBufferSize
    }
    // Only send updates if the state changed by more than 5%, more won't be
    // noticeable "from the outside" anyway. We send more frequent updates in
    // the gui/container of a block that needs it (like robots).
    val fillRatio = sumBuffer / sumBufferSize
    val shouldSend = math.abs(lastSentRatio - fillRatio) > (5.0 / 100.0)
    for (distributor <- distributors) distributor.synchronized {
      distributor.dirty = false
      distributor.globalBuffer = sumBuffer
      distributor.globalBufferSize = sumBufferSize
      distributor.owner.globalBuffer = sumBuffer
      distributor.owner.globalBufferSize = sumBufferSize
      if (shouldSend) {
        distributor.lastSentRatio = fillRatio
        ServerPacketSender.sendPowerState(distributor.owner)
      }
    }
  }
}
