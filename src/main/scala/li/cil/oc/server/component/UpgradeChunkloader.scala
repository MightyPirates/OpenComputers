package li.cil.oc.server.component

import java.util

import li.cil.oc.Constants
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute
import li.cil.oc.api.driver.DeviceInfo.DeviceClass
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
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ChunkloaderTicket
import net.minecraft.entity.Entity
import net.minecraft.server.MinecraftServer
import net.minecraft.world.ChunkCoordIntPair

import scala.collection.convert.WrapAsJava._
import scala.collection.convert.WrapAsScala._

class UpgradeChunkloader(val host: EnvironmentHost) extends prefab.ManagedEnvironment with DeviceInfo {
  override val node = api.Network.newNode(this, Visibility.Network).
    withComponent("chunkloader").
    withConnector().
    create()

  private final lazy val deviceInfo = Map(
    DeviceAttribute.Class -> DeviceClass.Generic,
    DeviceAttribute.Description -> "World stabilizer",
    DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
    DeviceAttribute.Product -> "Realizer9001-CL"
  )

  override def getDeviceInfo: util.Map[String, String] = deviceInfo

  var ticket: Option[ChunkloaderTicket] = None
  var isSuspend: Boolean = false
  private var ownerContext: Option[Context] = None

  override val canUpdate = true

  override def update() {
    super.update()
    if (host.world.getTotalWorldTime % Settings.get.tickFrequency == 0 && ticket.isDefined) {
      if (!node.tryChangeBuffer(-Settings.get.chunkloaderCost * Settings.get.tickFrequency)) {
        ticket.foreach(ticket => ticket.release())
        ticket = None
      }
      else if (host.isInstanceOf[Entity]) // Robot move events are not fired for entities (drones)
        updateLoadedChunk()
    }
  }

  @Callback(doc = """function():boolean -- Gets whether the chunkloader is currently active.""")
  def isActive(context: Context, args: Arguments): Array[AnyRef] = {
    checkOwnerContext(context)
    result(ticket.isDefined)
  }

  @Callback(doc = """function(enabled:boolean):boolean -- Enables or disables the chunkloader.""")
  def setActive(context: Context, args: Arguments): Array[AnyRef] = {
    checkOwnerContext(context)
    result(setActive(args.checkBoolean(0)))
  }

  private def checkOwnerContext(context: Context) {
    if (ownerContext.isEmpty || context.node != ownerContext.get.node) {
      throw new IllegalArgumentException("can only be used by the owning computer")
    }
  }

  override def onConnect(node: Node) {
    super.onConnect(node)
    if (ownerContext.isEmpty && node.host.isInstanceOf[Context] && node.canBeReachedFrom(this.node)) {
      if (Settings.get.chunkloaderLogLevel > 1)
        OpenComputers.log.info(s"[chunkloader] Connected: $this")
      ownerContext = Some(node.host.asInstanceOf[Context])
      ChunkloaderUpgradeHandler.chunkloaders += this
      restoreTicket()
    }
  }

  override def onDisconnect(node: Node) {
    super.onDisconnect(node)
    if (ownerContext.isDefined && (node == this.node || node.host.isInstanceOf[Context] && (node.host.asInstanceOf[Context] == ownerContext.get))) {
      if (Settings.get.chunkloaderLogLevel > 1)
        OpenComputers.log.info(s"[chunkloader] Disconnected: $this")
      ownerContext = None
      ChunkloaderUpgradeHandler.chunkloaders -= this
      if (host.isInstanceOf[Entity] && ticket.isDefined) { // request new ticket when drone travel to dimension
        releaseTicket()
        ChunkloaderUpgradeHandler.chunkloaders.find(
          loader => {
            loader.node.address == this.node.address && loader.getOwnerName == getOwnerName && loader.ticket.isEmpty
          }
        ).foreach(loader => loader.requestTicket())
      } else {
        releaseTicket()
      }
    }
  }

  override def onMessage(message: Message) {
    super.onMessage(message)
    if (ownerContext.isDefined && message.source.address == ownerContext.get.node.address) {
      if (message.name == "computer.stopped") {
        setActive(enabled = false)
      }
      else if (message.name == "computer.started") {
        setActive(enabled = true)
      }
    }
  }

  private def setActive(enabled: Boolean) = {
    if (enabled && ticket.isEmpty) {
      requestTicket()
    }
    else if (!enabled && ticket.isDefined) {
      releaseTicket()
    }
    ticket.isDefined
  }

  private def hostChunkCoord = {
    val blockPos = BlockPosition(host)
    new ChunkCoordIntPair(blockPos.x >> 4, blockPos.z >> 4)
  }

  private def hostCoord = {
    BlockPosition(host).toChunkCoordinates
  }

  def getOwnerName = {
    host match {
      case agent: li.cil.oc.api.internal.Agent =>
        Option(agent.ownerName)
      case _ => None
    }
  }

  def updateLoadedChunk() {
    ticket.foreach(ticket => {
      ticket.blockCoord = hostCoord
      if (isSuspend) {
        ticket.chunkList.foreach(chunkCoord => ticket.unforceChunk(chunkCoord))
      } else {
        val chunkloaderChunks = UpgradeChunkloader.chunks(hostChunkCoord)
        ticket.chunkList.foreach(chunkCoord => if (!chunkloaderChunks.contains(chunkCoord)) {
          ticket.unforceChunk(chunkCoord)
        })
        chunkloaderChunks.foreach(chunkCoord => if (!ticket.chunkList.contains(chunkCoord)) {
          ticket.forceChunk(chunkCoord)
        })
      }
    })
  }

  private def loaderInit() {
    ticket.foreach(ticket => {
      isSuspend = !UpgradeChunkloader.canBeAwakened(ticket)
      // duplicated chunkloaders will not work
      ChunkloaderUpgradeHandler.chunkloaders.foreach(
        loader => if (loader.node.address == node.address && loader != this) {
          loader.releaseTicket()
        }
      )
      if (Settings.get.chunkloaderLogLevel > 0)
        OpenComputers.log.info(s"[chunkloader] Activated: $this")
      updateLoadedChunk()
    })
  }

  private def restoreTicket() {
    if (ChunkloaderUpgradeHandler.restoredTickets.contains(node.address)) {
      OpenComputers.log.info(s"[chunkloader] Reclaiming chunk loader ticket for upgrade: $this")
      ticket = ChunkloaderUpgradeHandler.restoredTickets.remove(node.address)
      ticket.foreach(ticket => {
        ticket.unchecked = false
      })
      loaderInit()
    }
  }

  private def requestTicket() {
    val dim = host.world().provider.dimensionId
    ticket = {
      if (!UpgradeChunkloader.allowedDim(dim) || node.globalBuffer() < Settings.get.chunkloaderCost) {
        None
      } else if (!UpgradeChunkloader.playerTickets) {
        ChunkloaderTicket.requestTicket(host.world, node.address)
      } else {
        getOwnerName match {
          case Some(ownerName) =>
            if (ownerName != Settings.get.fakePlayerName)
              ChunkloaderTicket.requestPlayerTicket(host.world, node.address, ownerName)
            else
              None
          case None =>
            None
        }
      }
    }
    loaderInit()
  }

  private def releaseTicket() {
    ticket.foreach(ticket => {
      ticket.release()
      this.ticket = None
      if (Settings.get.chunkloaderLogLevel > 0)
        OpenComputers.log.info(s"[chunkloader] Deactivated: $this")
    })
  }

  override def toString = {
    val sAddress = s"${node.address}"
    val sActive = if (ticket.isDefined) ", active" else ", inactive"
    val sSuspend = if (ticket.isDefined && isSuspend) "/suspend" else ""
    val sOwner =  if (getOwnerName.isDefined) s", owned by ${getOwnerName.get}" else ""
    val sCoord = s", $hostCoord$hostChunkCoord/${host.world.provider.dimensionId}"
    s"chunkloader{$sAddress$sActive$sSuspend$sCoord$sOwner}"
  }
}

object UpgradeChunkloader {
  def allowedDim(dim: Int) = !(
    Settings.get.chunkloaderDimBlacklist.contains(dim) ||
      (Settings.get.chunkloaderDimWhitelist.size != 0 && !Settings.get.chunkloaderDimWhitelist.contains(dim))
    )

  def canBeAwakened(ticket: ChunkloaderTicket) =
    !Settings.get.chunkloaderRequireOnline || MinecraftServer.getServer.getAllUsernames.contains(ticket.ownerName.orNull)

  def chunks(centerChunkCoord: ChunkCoordIntPair) =
    (for (x <- -1 to 1; z <- -1 to 1) yield
      new ChunkCoordIntPair(centerChunkCoord.chunkXPos + x, centerChunkCoord.chunkZPos + z)).toSet

  def playerTickets =
    Settings.get.chunkloaderRequireOnline || Settings.get.chunkloaderPlayerTickets
}
