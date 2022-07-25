package li.cil.oc.common.event

import java.util.UUID

import li.cil.oc.OpenComputers
import li.cil.oc.api.event.RobotMoveEvent
import li.cil.oc.server.component.UpgradeChunkloader
import li.cil.oc.util.BlockPosition
import net.minecraft.util.math.ChunkPos
import net.minecraft.world.World
import net.minecraft.world.ForcedChunksSaveData
import net.minecraft.world.server.ServerWorld
import net.minecraftforge.common.world.ForgeChunkManager
import net.minecraftforge.common.world.ForgeChunkManager.LoadingValidationCallback
import net.minecraftforge.common.world.ForgeChunkManager.TicketHelper
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraft.entity.Entity

import scala.collection.convert.ImplicitConversionsToScala._
import scala.collection.immutable
import scala.collection.mutable

object ChunkloaderUpgradeHandler extends LoadingValidationCallback {
  private val restoredTickets = mutable.Map.empty[UUID, ChunkPos]

  private def parseAddress(addr: String): Option[UUID] = try {
    Some(UUID.fromString(addr))
  }
  catch {
    case _: RuntimeException => None
  }

  def claimTicket(addr: String) = parseAddress(addr).flatMap(restoredTickets.remove)

  override def validateTickets(world: ServerWorld, helper: TicketHelper) {
    for ((owner, ticketsPair) <- helper.getEntityTickets) {
      // This ensures that malformed tickets are also cleared on world save.
      restoredTickets += owner -> null
      // Chunkloaders use only ticking tickets.
      val tickets = ticketsPair.getSecond
      if (tickets.size == 9) {
        var (minX, minZ, maxX, maxZ) = (0, 0, 0, 0)
        for (combinedPos <- tickets) {
          val x = ChunkPos.getX(combinedPos)
          val z = ChunkPos.getZ(combinedPos)
          minX = minX min x
          maxX = maxX max x
          minZ = minZ min z
          maxZ = maxZ max z
        }
        if (minX + 2 == maxX && minZ + 2 == maxZ) {
          val x = minX + 1
          val z = minZ + 1
          OpenComputers.log.info(s"Restoring chunk loader ticket for upgrade at chunk ($x, $z) with address ${owner}.")
          restoredTickets += owner -> new ChunkPos(x, z)
        }
        else {
          OpenComputers.log.warn(s"Chunk loader ticket for $owner loads an incorrect shape.")
          helper.removeAllTickets(owner)
        }
      }
      else {
        OpenComputers.log.warn(s"Chunk loader ticket for $owner loads ${tickets.size} chunks.")
        helper.removeAllTickets(owner)
      }
    }
  }

  @SubscribeEvent
  def onWorldSave(e: WorldEvent.Save) = e.getWorld match {
    case world: ServerWorld => {
      // Any tickets that were not reassigned by the time the world gets saved
      // again can be considered orphaned, so we release them.
      // TODO figure out a better event *after* tile entities were restored
      // but *before* the world is saved, because the tickets are saved first,
      // so if the save is because the game is being quit the tickets aren't
      // actually being cleared. This will *usually* not be a problem, but it
      // has room for improvement.
      for ((owner, pos) <- restoredTickets) {
        try {
          OpenComputers.log.warn(s"A chunk loader ticket has been orphaned! Address: ${owner}, position: (${pos.x}, ${pos.z}). Removing...")
          releaseTicket(world, owner.toString, pos)
        }
        catch {
          case err: Throwable => OpenComputers.log.error(err)
        }
      }
      restoredTickets.clear()
    }
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

  def releaseTicket(world: ServerWorld, addr: String, pos: ChunkPos): Unit = parseAddress(addr) match {
    case Some(uuid) => {
      for (x <- -1 to 1; z <- -1 to 1) {
        ForgeChunkManager.forceChunk(world, OpenComputers.ID, uuid, pos.x + x, pos.z + z, false, true)
      }
    }
    case _ => OpenComputers.log.warn("Address '$addr' could not be parsed")
  }

  def updateLoadedChunk(loader: UpgradeChunkloader) {
    (loader.host.world, parseAddress(loader.node.address)) match {
      // If loader.ticket is None that means we shouldn't load anything (as did the old ticketing system).
      case (world: ServerWorld, Some(owner)) if loader.ticket.isDefined => {
        val blockPos = BlockPosition(loader.host)
        val centerChunk = new ChunkPos(blockPos.x >> 4, blockPos.z >> 4)
        if (centerChunk != loader.ticket.get) {
          val robotChunks = (for (x <- -1 to 1; z <- -1 to 1) yield new ChunkPos(centerChunk.x + x, centerChunk.z + z)).toSet
          val existingChunks = loader.ticket match {
            case Some(currPos) => (for (x <- -1 to 1; z <- -1 to 1) yield new ChunkPos(currPos.x + x, currPos.z + z)).toSet
            case None => immutable.Set.empty[ChunkPos]
          }
          for (toRemove <- existingChunks if !robotChunks.contains(toRemove)) {
            ForgeChunkManager.forceChunk(world, OpenComputers.ID, owner, toRemove.x, toRemove.z, false, true)
          }
          for (toAdd <- robotChunks if !existingChunks.contains(toAdd)) {
            ForgeChunkManager.forceChunk(world, OpenComputers.ID, owner, toAdd.x, toAdd.z, true, true)
          }
          loader.ticket = Some(centerChunk)
        }
      }
    }
  }
}
