package li.cil.oc.common.event

import java.util
import net.minecraft.world.{ChunkCoordIntPair, World}
import net.minecraftforge.common.ForgeChunkManager.{Ticket, LoadingCallback}
import scala.collection.convert.WrapAsScala._
import scala.collection.mutable
import li.cil.oc.api.event.RobotMoveEvent
import net.minecraftforge.event.ForgeSubscribe
import li.cil.oc.server.component.UpgradeChunkloader
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.common.ForgeChunkManager
import li.cil.oc.OpenComputers

object ChunkloaderUpgradeHandler extends LoadingCallback {
  val restoredTickets = mutable.Map.empty[String, Ticket]

  override def ticketsLoaded(tickets: util.List[Ticket], world: World) {
    for (ticket <- tickets) {
      restoredTickets += ticket.getModData.getString("address") -> ticket
    }
  }

  @ForgeSubscribe
  def onWorldSave(e: WorldEvent.Save) {
    // Any tickets that were not reassigned by the time the world gets saved
    // again can be considered orphaned, so we release them.
    // TODO figure out a better event *after* tile entities were restored
    // but *before* the world is saved, because the tickets are saved first,
    // so if the save is because the game is being quit the tickets aren't
    // actually being cleared. This will *usually* not be a problem, but it
    // has room for improvement.
    restoredTickets.values.foreach(ForgeChunkManager.releaseTicket)
    restoredTickets.clear()
  }

  // TODO it might be necessary to use pre move to force load the target chunk
  // in case the robot moves across a chunk border into an otherwise unloaded
  // chunk (I think it would just fail to move otherwise)

  @ForgeSubscribe
  def onMove(e: RobotMoveEvent.Post) {
    for (slot <- 0 until e.robot.getSizeInventory) {
      e.robot.getComponentInSlot(slot) match {
        case loader: UpgradeChunkloader => updateLoadedChunk(loader)
        case _ =>
      }
    }
  }

  def updateLoadedChunk(loader: UpgradeChunkloader) {
    val robotChunk = new ChunkCoordIntPair(loader.robot.xCoord / 16, loader.robot.zCoord / 16)
    loader.ticket.foreach(ticket => {
      ticket.getChunkList.collect {
        case chunk: ChunkCoordIntPair if chunk != robotChunk => ForgeChunkManager.unforceChunk(ticket, chunk)
      }
      ForgeChunkManager.forceChunk(ticket, robotChunk)
      ticket.getModData.setString("address", loader.node.address)
    })
  }
}
