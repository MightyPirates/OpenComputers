package li.cil.oc.server.component

import li.cil.oc.{Settings, OpenComputers}
import li.cil.oc.api
import li.cil.oc.api.network._
import li.cil.oc.common.component
import li.cil.oc.common.event.ChunkloaderUpgradeHandler
import net.minecraftforge.common.ForgeChunkManager
import net.minecraftforge.common.ForgeChunkManager.Ticket
import li.cil.oc.api.driver.Container

class UpgradeChunkloader(val owner: Container) extends component.ManagedComponent {
  val node = api.Network.newNode(this, Visibility.Network).
    withComponent("chunkloader").
    withConnector().
    create()

  var ticket: Option[Ticket] = None

  override val canUpdate = true

  override def update() {
    super.update()
    if (owner.world.getWorldTime % Settings.get.tickFrequency == 0 && ticket.isDefined) {
      if (!node.tryChangeBuffer(-Settings.get.chunkloaderCost * Settings.get.tickFrequency)) {
        ticket.foreach(ForgeChunkManager.releaseTicket)
        ticket = None
      }
    }
  }

  @Callback
  def isActive(context: Context, args: Arguments): Array[AnyRef] = result(ticket.isDefined)

  @Callback
  def setActive(context: Context, args: Arguments): Array[AnyRef] = {
    val enabled = args.checkBoolean(0)
    if (enabled && ticket.isEmpty) {
      ticket = Option(ForgeChunkManager.requestTicket(OpenComputers, owner.world, ForgeChunkManager.Type.NORMAL))
      ChunkloaderUpgradeHandler.updateLoadedChunk(this)
    }
    else if (!enabled && ticket.isDefined) {
      ticket.foreach(ForgeChunkManager.releaseTicket)
      ticket = None
    }
    result(ticket.isDefined)
  }

  override def onConnect(node: Node) {
    super.onConnect(node)
    if (node == this.node) {
      ticket = ChunkloaderUpgradeHandler.restoredTickets.remove(node.address).
        orElse(Option(ForgeChunkManager.requestTicket(OpenComputers, owner.world, ForgeChunkManager.Type.NORMAL)))
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
}
