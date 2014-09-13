package li.cil.oc.server.component

import li.cil.oc.api.driver.Container
import li.cil.oc.api.network._
import li.cil.oc.common.component
import li.cil.oc.common.event.ChunkloaderUpgradeHandler
import li.cil.oc.{OpenComputers, Settings, api}
import net.minecraftforge.common.ForgeChunkManager
import net.minecraftforge.common.ForgeChunkManager.Ticket

class UpgradeChunkloader(val owner: Container) extends component.ManagedComponent {
  val node = api.Network.newNode(this, Visibility.Network).
    withComponent("chunkloader").
    withConnector().
    create()

  var ticket: Option[Ticket] = None

  override val canUpdate = true

  override def update() {
    super.update()
    if (owner.world.getTotalWorldTime % Settings.get.tickFrequency == 0 && ticket.isDefined) {
      if (!node.tryChangeBuffer(-Settings.get.chunkloaderCost * Settings.get.tickFrequency)) {
        ticket.foreach(ForgeChunkManager.releaseTicket)
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
      ticket = ChunkloaderUpgradeHandler.restoredTickets.remove(node.address).
        orElse(owner match {
        case context: Context if context.isRunning => Option(ForgeChunkManager.requestTicket(OpenComputers, owner.world, ForgeChunkManager.Type.NORMAL))
        case _ => None
      })
      ChunkloaderUpgradeHandler.updateLoadedChunk(this)
    }
  }

  override def onDisconnect(node: Node) {
    super.onDisconnect(node)
    if (node == this.node) {
      ticket.foreach(ForgeChunkManager.releaseTicket)
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
      ticket = Option(ForgeChunkManager.requestTicket(OpenComputers, owner.world, ForgeChunkManager.Type.NORMAL))
      ChunkloaderUpgradeHandler.updateLoadedChunk(this)
    }
    else if (!enabled && ticket.isDefined) {
      ticket.foreach(ForgeChunkManager.releaseTicket)
      ticket = None
    }
    ticket.isDefined
  }
}
