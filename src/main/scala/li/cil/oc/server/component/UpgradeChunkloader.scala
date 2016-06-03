package li.cil.oc.server.component

import java.util

import li.cil.oc.Constants
import li.cil.oc.Constants.DeviceInfo.DeviceAttribute
import li.cil.oc.Constants.DeviceInfo.DeviceClass
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.driver.DeviceInfo
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.api.network._
import li.cil.oc.api.prefab
import li.cil.oc.common.event.ChunkloaderUpgradeHandler
import net.minecraftforge.common.ForgeChunkManager
import net.minecraftforge.common.ForgeChunkManager.Ticket

import scala.collection.convert.WrapAsJava._

class UpgradeChunkloader(val host: EnvironmentHost) extends prefab.ManagedEnvironment with DeviceInfo {
  override val node = api.Network.newNode(this, Visibility.Network).
    withComponent("chunkloader").
    withConnector().
    create()

  private final val deviceInfo = Map(
    DeviceAttribute.Class -> DeviceClass.Generic,
    DeviceAttribute.Description -> "World stabilizer",
    DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
    DeviceAttribute.Product -> "Realizer9001-CL"
  )

  override def getDeviceInfo: util.Map[String, String] = deviceInfo

  var ticket: Option[Ticket] = None

  override val canUpdate = true

  override def update() {
    super.update()
    if (host.world.getTotalWorldTime % Settings.get.tickFrequency == 0 && ticket.isDefined) {
      if (!node.tryChangeBuffer(-Settings.get.chunkloaderCost * Settings.get.tickFrequency)) {
        ticket.foreach(ticket => try ForgeChunkManager.releaseTicket(ticket) catch {
          case _: Throwable => // Ignored.
        })
        ticket = None
      }
    }
  }

  @Callback(doc = """function():boolean -- Gets whether the chunkloader is currently active.""")
  def isActive(context: Context, args: Arguments): Array[AnyRef] = result(ticket.isDefined)

  @Callback(doc = """function(enabled:boolean):boolean -- Enables or disables the chunkloader.""")
  def setActive(context: Context, args: Arguments): Array[AnyRef] = result(setActive(args.checkBoolean(0)))

  override def onConnect(node: Node) {
    super.onConnect(node)
    if (node == this.node) {
      if (ChunkloaderUpgradeHandler.restoredTickets.contains(node.address)) {
        OpenComputers.log.info(s"Reclaiming chunk loader ticket at (${host.xPosition()}, ${host.yPosition()}, ${host.zPosition()}) in dimension ${host.world().provider.dimensionId}.")
      }
      ticket = ChunkloaderUpgradeHandler.restoredTickets.remove(node.address).orElse(host match {
        case context: Context if context.isRunning => Option(ForgeChunkManager.requestTicket(OpenComputers, host.world, ForgeChunkManager.Type.NORMAL))
        case _ => None
      })
      ChunkloaderUpgradeHandler.updateLoadedChunk(this)
    }
  }

  override def onDisconnect(node: Node) {
    super.onDisconnect(node)
    if (node == this.node) {
      ticket.foreach(ticket => try ForgeChunkManager.releaseTicket(ticket) catch {
        case _: Throwable => // Ignored.
      })
      ticket = None
    }
  }

  override def onMessage(message: Message) {
    super.onMessage(message)
    if (message.name == "computer.stopped") {
      setActive(enabled = false)
    }
    else if (message.name == "computer.started") {
      setActive(enabled = true)
    }
  }

  private def setActive(enabled: Boolean) = {
    if (enabled && ticket.isEmpty) {
      ticket = Option(ForgeChunkManager.requestTicket(OpenComputers, host.world, ForgeChunkManager.Type.NORMAL))
      ChunkloaderUpgradeHandler.updateLoadedChunk(this)
    }
    else if (!enabled && ticket.isDefined) {
      ticket.foreach(ticket => try ForgeChunkManager.releaseTicket(ticket) catch {
        case _: Throwable => // Ignored.
      })
      ticket = None
    }
    ticket.isDefined
  }
}
