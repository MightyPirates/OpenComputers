package li.cil.oc.common.event

import java.util

import li.cil.oc.api.event.RobotMoveEvent
import li.cil.oc.server.component.UpgradeChunkloader
import net.minecraft.world.{ChunkCoordIntPair, World}
import net.minecraftforge.common.ForgeChunkManager
import net.minecraftforge.common.ForgeChunkManager.{LoadingCallback, Ticket}
import net.minecraftforge.event.ForgeSubscribe
import net.minecraftforge.event.world.WorldEvent

import scala.collection.convert.WrapAsScala._
import scala.collection.mutable

object ChunkloaderUpgradeHandler extends LoadingCallback {
  val restoredTickets = mutable.Map.empty[String, Ticket]

  override def ticketsLoaded(tickets: util.List[Ticket], world: World) {
    for (ticket <- tickets) {
      val data = ticket.getModData
      restoredTickets += data.getString("address") -> ticket
      if (data.hasKey("x") && data.hasKey("z")) {
        val x = data.getInteger("x")
        val z = data.getInteger("z")
        ForgeChunkManager.forceChunk(ticket, new ChunkCoordIntPair(x, z))
      }
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

  // Note: it might be necessary to use pre move to force load the target chunk
  // in case the robot moves across a chunk border into an otherwise unloaded
  // chunk (I think it would just fail to move otherwise).
  // Update 2014-06-21: did some testing, seems not to be necessary. My guess
  // is that the access to the block in the direction the robot moves causes
  // the chunk it might move into to get loaded.

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
    val robotChunk = new ChunkCoordIntPair(math.round(loader.owner.xPosition - 0.5).toInt >> 4, math.round(loader.owner.zPosition - 0.5).toInt >> 4)
    loader.ticket.foreach(ticket => {
      ticket.getChunkList.collect {
        case chunk: ChunkCoordIntPair if chunk != robotChunk => ForgeChunkManager.unforceChunk(ticket, chunk)
      }
      ForgeChunkManager.forceChunk(ticket, robotChunk)
      ticket.getModData.setString("address", loader.node.address)
      ticket.getModData.setInteger("x", robotChunk.chunkXPos)
      ticket.getModData.setInteger("z", robotChunk.chunkZPos)
    })
  }
}
