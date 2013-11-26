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

  private var lastSentRatio = 0.0

  private val buffers = mutable.Set.empty[Connector]

  private val distributors = mutable.Set.empty[PowerDistributor]

  private var dirty = true

  // ----------------------------------------------------------------------- //

  @LuaCallback(value = "buffer", direct = true)
  def buffer(context: Context, args: Arguments): Array[AnyRef] = result(globalBuffer)

  @LuaCallback(value = "bufferSize", direct = true)
  def bufferSize(context: Context, args: Arguments): Array[AnyRef] = result(globalBufferSize)

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
    this.synchronized {
      val oldBuffer = globalBuffer
      globalBuffer = (globalBuffer + delta) max 0 min globalBufferSize
      if (globalBuffer == oldBuffer) {
        return delta
      }
      dirty = true
      if (delta < 0) {
        var remaining = -delta
        for (connector <- buffers) {
          if (connector.localBuffer > 0) {
            connector.dirty = true
            if (connector.localBuffer < remaining) {
              remaining -= connector.localBuffer
              connector.localBuffer = 0
            }
            else {
              connector.localBuffer -= remaining
              return 0
            }
          }
        }
        remaining
      }
      else /* if (delta > 0) */ {
        var remaining = delta
        for (connector <- buffers) {
          if (connector.localBuffer < connector.localBufferSize) {
            connector.dirty = true
            val space = connector.localBufferSize - connector.localBuffer
            if (space < remaining) {
              remaining -= space
              connector.localBuffer = connector.localBufferSize
            }
            else {
              connector.localBuffer += remaining
              return 0
            }
          }
        }
        remaining
      }
    }
  }

  // ----------------------------------------------------------------------- //

  override def update() {
    if (node != null && (dirty || buffers.exists(_.dirty))) {
      updateCachedValues()
    }
  }

  // ----------------------------------------------------------------------- //

  override def onConnect(node: Node) {
    super.onConnect(node)
    if (node == this.node) {
      for (node <- node.reachableNodes) node match {
        case connector: Connector if connector.localBufferSize > 0 => this.synchronized {
          buffers += connector
          globalBuffer += connector.localBuffer
          globalBufferSize += connector.localBufferSize
        }
        case _ => node.host match {
          case distributor: PowerDistributor if distributor.node.canBeSeenFrom(this.node) =>
            distributors += distributor
          case _ =>
        }
      }
      distributors += this
      dirty = true
    }
    else node match {
      case connector: Connector if connector.localBufferSize > 0 => this.synchronized {
        buffers += connector
        globalBuffer += connector.localBuffer
        globalBufferSize += connector.localBufferSize
        dirty = true
      }
      case _ => node.host match {
        case distributor: PowerDistributor if distributor.node.canBeSeenFrom(this.node) =>
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
    val (sumBuffer, sumBufferSize) =
      buffers.foldRight((0.0, 0.0))((c, acc) => {
        c.dirty = false // clear dirty flag for all connectors
        (acc._1 + c.localBuffer, acc._2 + c.localBufferSize)
      })
    // Only send updates if the state changed by more than 5%, more won't be
    // noticeable "from the outside" anyway. We send more frequent updates in
    // the gui/container of a block that needs it (like robots).
    val fillRatio = sumBuffer / sumBufferSize
    val shouldSend = (lastSentRatio - fillRatio).abs > (5.0 / 100.0)
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
