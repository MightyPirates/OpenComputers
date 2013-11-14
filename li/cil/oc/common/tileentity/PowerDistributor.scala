package li.cil.oc.common.tileentity

import li.cil.oc.api.network._
import li.cil.oc.client.{PacketSender => ClientPacketSender}
import li.cil.oc.server.network.Connector
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import li.cil.oc.{Config, api}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import scala.collection.convert.WrapAsScala._
import scala.collection.mutable

class PowerDistributor extends Environment with Analyzable {
  val node = api.Network.newNode(this, Visibility.Network).
    withComponent("power", Visibility.Network).
    create()

  var globalBuffer = 0.0

  var globalBufferSize = 0.0

  var average = 0.0

  private var lastSentAverage = 0.0

  private val buffers = mutable.Set.empty[Connector]

  private val distributors = mutable.Set.empty[PowerDistributor]

  private var dirty = true

  // ----------------------------------------------------------------------- //

  @LuaCallback(value = "buffer", direct = true)
  def buffer(context: Context, args: Arguments): Array[AnyRef] = result(globalBuffer)

  @LuaCallback(value = "bufferSize", direct = true)
  def bufferSize(context: Context, args: Arguments): Array[AnyRef] = result(globalBufferSize)

  // ----------------------------------------------------------------------- //

  def onAnalyze(stats: NBTTagCompound, player: EntityPlayer, side: Int, hitX: Float, hitY: Float, hitZ: Float) = {
    stats.setString(Config.namespace + "text.Analyzer.TotalEnergy", "%.2f/%.2f".format(globalBuffer, globalBufferSize))
    this
  }

  // ----------------------------------------------------------------------- //

  def changeBuffer(delta: Double): Boolean = {
    if (delta != 0) {
      val oldBuffer = globalBuffer
      globalBuffer = (globalBuffer + delta) max 0 min globalBufferSize
      if (globalBuffer != oldBuffer) {
        dirty = true
        if (delta < 0) {
          var remaining = -delta
          for (connector <- buffers if connector.localBuffer > 0) {
            if (connector.localBuffer < remaining) {
              remaining -= connector.localBuffer
              connector.localBuffer = 0
            }
            else {
              connector.changeBuffer(-remaining)
              return true
            }
          }
        }
        else if (delta > 0) {
          var remaining = delta
          for (connector <- buffers if connector.localBuffer < connector.localBufferSize) {
            val space = connector.localBufferSize - connector.localBuffer
            if (space < remaining) {
              remaining -= space
              connector.localBuffer = connector.localBufferSize
            }
            else {
              connector.changeBuffer(remaining)
              return true
            }
          }
        }
      }
    }
    false
  }

  // ----------------------------------------------------------------------- //

  override def updateEntity() {
    super.updateEntity()
    if (!worldObj.isRemote && (dirty || buffers.exists(_.dirty))) {
      updateCachedValues()
    }
  }

  override def validate() {
    super.validate()
    if (worldObj.isRemote) {
      ClientPacketSender.sendPowerStateRequest(this)
    }
  }

  // ----------------------------------------------------------------------- //

  override def onConnect(node: Node) {
    super.onConnect(node)
    if (node == this.node) {
      for (node <- node.network.nodes) node match {
        case connector: Connector if connector.localBufferSize > 0 =>
          buffers += connector
          globalBuffer += connector.localBuffer
          globalBufferSize += connector.localBufferSize
        case _ => node.host match {
          case distributor: PowerDistributor => distributors += distributor
          case _ =>
        }
      }
      dirty = true
    }
    else node match {
      case connector: Connector =>
        buffers += connector
        globalBuffer += connector.localBuffer
        globalBufferSize += connector.localBufferSize
        dirty = true
      case _ => node.host match {
        case distributor: PowerDistributor => distributors += distributor
        case _ =>
      }
    }
  }

  override def onDisconnect(node: Node) {
    super.onDisconnect(node)
    if (node == this.node) {
      buffers.clear()
      distributors.clear()
      globalBuffer = 0
      globalBufferSize = 0
      average = -1
    }
    else node match {
      case connector: Connector =>
        buffers -= connector
        globalBuffer -= connector.localBuffer
        globalBufferSize -= connector.localBufferSize
        dirty = true
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
    average = if (globalBufferSize > 0) globalBuffer / globalBufferSize else 0
    val shouldSend = (lastSentAverage - average).abs > 0.05
    for (distributor <- distributors) {
      distributor.dirty = false
      distributor.globalBuffer = sumBuffer
      distributor.globalBufferSize = sumBufferSize
      distributor.average = average
      if (shouldSend) {
        distributor.lastSentAverage = lastSentAverage
        ServerPacketSender.sendPowerState(distributor)
      }
    }
  }
}
