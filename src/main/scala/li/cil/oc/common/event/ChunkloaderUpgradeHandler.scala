package li.cil.oc.common.event

import java.util

import cpw.mods.fml.common.eventhandler.{EventPriority, SubscribeEvent}
import li.cil.oc.util.ChunkloaderTicket
import li.cil.oc.{OpenComputers, Settings}
import li.cil.oc.api.event.RobotMoveEvent
import li.cil.oc.server.component.UpgradeChunkloader
import net.minecraft.world.World
import net.minecraftforge.common.ForgeChunkManager
import net.minecraftforge.common.ForgeChunkManager.OrderedLoadingCallback
import net.minecraftforge.common.ForgeChunkManager.PlayerOrderedLoadingCallback
import net.minecraftforge.event.world.{ChunkEvent, WorldEvent}
import com.google.common.collect.ListMultimap
import com.google.common.collect.ArrayListMultimap
import cpw.mods.fml.common.gameevent.PlayerEvent
import net.minecraft.server.MinecraftServer

import scala.collection.convert.WrapAsScala._
import scala.collection.mutable

object ChunkloaderUpgradeHandler extends OrderedLoadingCallback with PlayerOrderedLoadingCallback {
  val chunkloaders = mutable.Set.empty[UpgradeChunkloader]
  val restoredTickets = mutable.Map.empty[String, ChunkloaderTicket]

  override def playerTicketsLoaded(tickets: ListMultimap[String, ForgeChunkManager.Ticket], world: World) = {
    val loaded: ListMultimap[String, ForgeChunkManager.Ticket] = ArrayListMultimap.create()
    if (UpgradeChunkloader.playerTickets && UpgradeChunkloader.allowedDim(world.provider.dimensionId)) {
      for (e <- tickets.entries) {
        val ticket = e.getValue
        if (isValidTicket(ticket)) {
          loaded.put(ticket.getPlayerName, ticket)
        }
      }
    }
    loaded
  }

  override def ticketsLoaded(tickets: util.List[ForgeChunkManager.Ticket], world: World, maxTicketCount: Int) = {
    val loaded = new util.ArrayList[ForgeChunkManager.Ticket]
    if (!UpgradeChunkloader.playerTickets && UpgradeChunkloader.allowedDim(world.provider.dimensionId)) {
      for (ticket <- tickets) {
        if (isValidTicket(ticket)) {
          loaded.add(ticket)
        }
      }
    }
    loaded
  }

  private def isValidTicket(ticket: ForgeChunkManager.Ticket) = {
    val data = ticket.getModData
    // omit malformed tickets
    data.hasKey("x") && data.hasKey("y") && data.hasKey("z") && data.hasKey("address")
  }

  override def ticketsLoaded(tickets: util.List[ForgeChunkManager.Ticket], world: World) {
    for (fcmTicket <- tickets) {
      val ticket = new ChunkloaderTicket(fcmTicket)
      restoredTickets += ticket.address -> ticket
      OpenComputers.log.info(s"[chunkloader] Restoring: $ticket")
      if (UpgradeChunkloader.canBeAwakened(ticket)) {
        forceTicketChunks(ticket)
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
    restoredTickets.values.foreach(ticket => {
      if (ticket.unchecked) {
        OpenComputers.log.warn(s"[chunkloader] Removing orphaned: $ticket")
        ticket.release()
        restoredTickets -= ticket.address
      }
    })
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
      case loader: UpgradeChunkloader => loader.updateLoadedChunk()
      case _ =>
    })
  }

  object PersonalHandler {
    val unloadedTickets = mutable.Map.empty[String, ChunkloaderTicket]

    @SubscribeEvent(priority = EventPriority.HIGHEST) // before UpgradeChunkloader.onDisconnect
    def onChunkUnload(e: ChunkEvent.Unload) {
      val chunkCoord = e.getChunk.getChunkCoordIntPair
      val dim = e.getChunk.worldObj.provider.dimensionId
      chunkloaders.foreach(loader => {
        loader.ticket.foreach(ticket => if (ticket.dim == dim && ticket.chunkCoord == chunkCoord) {
          restoredTickets += ticket.address -> ticket
          if (Settings.get.chunkloaderLogLevel > 0)
            OpenComputers.log.info(s"[chunkloader] Unloading: $loader")
          loader.ticket = None // prevent release
        })
      })
    }

    @SubscribeEvent(priority = EventPriority.HIGH) // after ForgeChunkManager
    def onWorldLoad(e: WorldEvent.Load) {
      unloadedTickets.retain((_, ticket) => ticket.dim != e.world.provider.dimensionId)
    }

    @SubscribeEvent(priority = EventPriority.HIGH) // after ForgeChunkManager, before UpgradeChunkloader.onDisconnect
    def onWorldUnload(e: WorldEvent.Unload) {
      val dim = e.world.provider.dimensionId
      restoredTickets.values.foreach(ticket => if(ticket.dim == dim) {
        OpenComputers.log.info(s"[chunkloader] Unloading: $ticket")
        ticket.ownerName.foreach(ownerName => {
          unloadedTickets += ticket.address -> ticket
        })
      })
      restoredTickets.retain((_, ticket) => ticket.dim != dim)
    }

    @SubscribeEvent
    def onPlayerLoggedIn(e: PlayerEvent.PlayerLoggedInEvent) {
      // awake chunk loaders
      chunkloaders.foreach(loader =>
        loader.getOwnerName.foreach(ownerName => {
          if (ownerName == e.player.getCommandSenderName) {
            if (Settings.get.chunkloaderLogLevel > 0)
              OpenComputers.log.info(s"[chunkloader] Awake: $loader")
            loader.isSuspend = false
            loader.updateLoadedChunk()
          }
        })
      )
      // force unloaded tickets
      restoredTickets.values.foreach(ticket =>
        ticket.ownerName.foreach(ownerName => {
          if (ownerName == e.player.getCommandSenderName) {
            forceTicketChunks(ticket)
          }
        })
      )
      // load unloaded dimensions
      val unloadedDims = mutable.Set.empty[Int]
      unloadedTickets.values.foreach(ticket => {
        ticket.ownerName.foreach(ownerName => if (ownerName == e.player.getCommandSenderName) {
          unloadedDims += ticket.dim
        })
      })
      unloadedDims.foreach(dim => {
        val world = MinecraftServer.getServer.worldServerForDimension(dim)
        if (world == null) {
          OpenComputers.log.warn(s"[chunkloader] Could not load dimension $dim")
        }
      })
    }

    @SubscribeEvent
    def onPlayerLoggedOut(e: PlayerEvent.PlayerLoggedOutEvent) {
      // suspend chunk loaders
      chunkloaders.foreach(loader =>
        loader.getOwnerName.foreach(ownerName => if (ownerName == e.player.getCommandSenderName) {
          if (Settings.get.chunkloaderLogLevel > 0)
            OpenComputers.log.info(s"[chunkloader] Suspend: $loader")
          loader.isSuspend = true
          loader.updateLoadedChunk()
        })
      )
    }
  }

  def forceTicketChunks(ticket: ChunkloaderTicket) {
    if (Settings.get.chunkloaderLogLevel > 1)
      OpenComputers.log.info(s"[chunkloader] Force: $ticket")
    ticket.unchecked = true
    UpgradeChunkloader.chunks(ticket.chunkCoord).foreach(chunkCoord => ticket.forceChunk(chunkCoord))
  }
}
