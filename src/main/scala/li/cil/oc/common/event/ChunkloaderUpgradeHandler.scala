package li.cil.oc.common.event

import java.util

import li.cil.oc.OpenComputers
import li.cil.oc.api.event.RobotMoveEvent
import li.cil.oc.server.component.UpgradeChunkloader
import li.cil.oc.util.BlockPosition
import net.minecraft.world.ChunkCoordIntPair
import net.minecraft.world.World
import net.minecraftforge.common.ForgeChunkManager
import net.minecraftforge.common.ForgeChunkManager.LoadingCallback
import net.minecraftforge.common.ForgeChunkManager.Ticket
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

import scala.collection.convert.WrapAsScala._
import scala.collection.mutable

object ChunkloaderUpgradeHandler extends LoadingCallback {
  val restoredTickets = mutable.Map.empty[String, Ticket]

  override def ticketsLoaded(tickets: util.List[Ticket], world: World) {
    for (ticket <- tickets) {
      val data = ticket.getModData
      val address = data.getString("address")
      restoredTickets += address -> ticket
      if (data.hasKey("x") && data.hasKey("z")) {
        val x = data.getInteger("x")
        val z = data.getInteger("z")
        OpenComputers.log.info(s"Restoring chunk loader ticket for upgrade at chunk ($x, $z) with address $address.")

        ForgeChunkManager.forceChunk(ticket, new ChunkCoordIntPair(x, z))
      }
    }
  }

  @SubscribeEvent
  def onWorldSave(e: WorldEvent.Save) {
    // Any tickets that were not reassigned by the time the world gets saved
    // again can be considered orphaned, so we release them.
    // TODO figure out a better event *after* tile entities were restored
    // but *before* the world is saved, because the tickets are saved first,
    // so if the save is because the game is being quit the tickets aren't
    // actually being cleared. This will *usually* not be a problem, but it
    // has room for improvement.
    restoredTickets.values.foreach(ticket => try ForgeChunkManager.releaseTicket(ticket) catch {
      case _: Throwable => // Ignored.
    })
    restoredTickets.clear()
  }

  // Note: it might be necessary to use pre move to force load the target chunk
  // in case the robot moves across a chunk border into an otherwise unloaded
  // chunk (I think it would just fail to move otherwise).
  // Update 2014-06-21: did some testing, seems not to be necessary. My guess
  // is that the access to the block in the direction the robot moves causes
  // the chunk it might move into to get loaded.

  @SubscribeEvent
  def onMove(e: RobotMoveEvent.Post) {
    val machineNode = e.agent.machine.node
    machineNode.reachableNodes.foreach(_.host match {
      case loader: UpgradeChunkloader => updateLoadedChunk(loader)
      case _ =>
    })
  }

  def updateLoadedChunk(loader: UpgradeChunkloader) {
    val blockPos = BlockPosition(loader.host)
    val centerChunk = new ChunkCoordIntPair(blockPos.x >> 4, blockPos.z >> 4)
    val robotChunks = (for (x <- -1 to 1; z <- -1 to 1) yield new ChunkCoordIntPair(centerChunk.chunkXPos + x, centerChunk.chunkZPos + z)).toSet

    loader.ticket.foreach(ticket => {
      ticket.getChunkList.collect {
        case chunk: ChunkCoordIntPair if !robotChunks.contains(chunk) => ForgeChunkManager.unforceChunk(ticket, chunk)
      }

      for (chunk <- robotChunks) {
        ForgeChunkManager.forceChunk(ticket, chunk)
      }

      ticket.getModData.setString("address", loader.node.address)
      ticket.getModData.setInteger("x", centerChunk.chunkXPos)
      ticket.getModData.setInteger("z", centerChunk.chunkZPos)
    })
  }
}
