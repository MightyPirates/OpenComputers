package li.cil.oc.server.network

import li.cil.oc.Settings
import li.cil.oc.common.tileentity.Waypoint
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.RTree
import net.minecraft.util.RegistryKey
import net.minecraft.world.World
import net.minecraftforge.event.world.ChunkEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.eventbus.api.SubscribeEvent

import scala.collection.convert.ImplicitConversionsToScala._
import scala.collection.mutable

object Waypoints {
  val dimensions = mutable.Map.empty[RegistryKey[World], RTree[Waypoint]]

  @SubscribeEvent
  def onWorldUnload(e: WorldEvent.Unload) {
    if (!e.getWorld.isClientSide) {
      e.getWorld match {
        case world: World => dimensions.remove(world.dimension)
        case _ =>
      }
    }
  }

  @SubscribeEvent
  def onWorldLoad(e: WorldEvent.Load) {
    if (!e.getWorld.isClientSide) {
      e.getWorld match {
        case world: World => dimensions.remove(world.dimension)
        case _ =>
      }
    }
  }

  // Safety clean up, in case some tile entities didn't properly leave the net.
  @SubscribeEvent
  def onChunkUnloaded(e: ChunkEvent.Unload) {
    e.getChunk.getBlockEntitiesPos.map(e.getChunk.getBlockEntity).foreach {
      case waypoint: Waypoint => remove(waypoint)
      case _ =>
    }
  }

  def add(waypoint: Waypoint): Unit = if (!waypoint.isRemoved && waypoint.world != null && !waypoint.world.isClientSide) {
    dimensions.getOrElseUpdate(dimension(waypoint), new RTree[Waypoint](Settings.get.rTreeMaxEntries)((waypoint) => (waypoint.x + 0.5, waypoint.y + 0.5, waypoint.z + 0.5))).add(waypoint)
  }

  def remove(waypoint: Waypoint): Unit = if (waypoint.world != null && !waypoint.world.isClientSide) {
    dimensions.get(dimension(waypoint)) match {
      case Some(set) => set.remove(waypoint)
      case _ =>
    }
  }

  def findWaypoints(pos: BlockPosition, range: Double): Iterable[Waypoint] = {
    dimensions.get(pos.world.get.dimension) match {
      case Some(set) =>
        val bounds = pos.bounds.inflate(range * 0.5, range * 0.5, range * 0.5)
        set.query((bounds.minX, bounds.minY, bounds.minZ), (bounds.maxX, bounds.maxY, bounds.maxZ))
      case _ => Iterable.empty
    }
  }

  private def dimension(waypoint: Waypoint) = waypoint.world.dimension
}
