package li.cil.oc.server.network

import li.cil.oc.Settings
import li.cil.oc.common.tileentity.{TileEntityWaypoint, Waypoint}
import li.cil.oc.util.RTree
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraftforge.event.world.ChunkEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

import scala.collection.convert.WrapAsScala._
import scala.collection.mutable

object Waypoints {
  val dimensions = mutable.Map.empty[Int, RTree[TileEntityWaypoint]]

  @SubscribeEvent
  def onWorldUnload(e: WorldEvent.Unload) {
    if (!e.getWorld.isRemote) {
      dimensions.remove(e.getWorld.provider.getDimension)
    }
  }

  @SubscribeEvent
  def onWorldLoad(e: WorldEvent.Load) {
    if (!e.getWorld.isRemote) {
      dimensions.remove(e.getWorld.provider.getDimension)
    }
  }

  // Safety clean up, in case some tile entities didn't properly leave the net.
  @SubscribeEvent
  def onChunkUnload(e: ChunkEvent.Unload) {
    e.getChunk.getTileEntityMap.values.foreach {
      case waypoint: TileEntityWaypoint => remove(waypoint)
      case _ =>
    }
  }

  def add(waypoint: TileEntityWaypoint): Unit = if (!waypoint.isInvalid && waypoint.getWorld != null && !waypoint.getWorld.isRemote) {
    dimensions.getOrElseUpdate(dimension(waypoint), new RTree[TileEntityWaypoint](Settings.get.rTreeMaxEntries)((waypoint) => (waypoint.x + 0.5, waypoint.y + 0.5, waypoint.z + 0.5))).add(waypoint)
  }

  def remove(waypoint: TileEntityWaypoint): Unit = if (waypoint.getWorld != null && !waypoint.getWorld.isRemote) {
    dimensions.get(dimension(waypoint)) match {
      case Some(set) => set.remove(waypoint)
      case _ =>
    }
  }

  def findWaypoints(world: World, pos: BlockPos, range: Double): Iterable[TileEntityWaypoint] = {
    dimensions.get(world.provider.getDimension) match {
      case Some(set) =>
        val bounds = pos.bounds.expand(range * 0.5, range * 0.5, range * 0.5)
        set.query((bounds.minX, bounds.minY, bounds.minZ), (bounds.maxX, bounds.maxY, bounds.maxZ))
      case _ => Iterable.empty
    }
  }

  private def dimension(waypoint: TileEntityWaypoint) = waypoint.getWorld.provider.getDimension
}
