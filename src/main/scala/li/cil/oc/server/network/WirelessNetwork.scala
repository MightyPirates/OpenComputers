package li.cil.oc.server.network

import li.cil.oc.Settings
import li.cil.oc.api.network.WirelessEndpoint
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedBlock._
import li.cil.oc.util.ExtendedWorld._
import li.cil.oc.util.RTree
import net.minecraft.util.RegistryKey
import net.minecraft.util.math.vector.Vector3d
import net.minecraft.world.World
import net.minecraftforge.event.world.ChunkEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.eventbus.api.SubscribeEvent

import scala.collection.convert.WrapAsScala._
import scala.collection.mutable

object WirelessNetwork {
  val dimensions = mutable.Map.empty[RegistryKey[World], RTree[WirelessEndpoint]]

  @SubscribeEvent
  def onWorldUnload(e: WorldEvent.Unload) {
    if (!e.getWorld.isClientSide) e.getWorld match {
      case world: World => dimensions.remove(world.dimension)
      case _ =>
    }
  }

  @SubscribeEvent
  def onWorldLoad(e: WorldEvent.Load) {
    if (!e.getWorld.isClientSide) e.getWorld match {
      case world: World => dimensions.remove(world.dimension)
      case _ =>
    }
  }

  // Safety clean up, in case some tile entities didn't properly leave the net.
  @SubscribeEvent
  def onChunkUnloaded(e: ChunkEvent.Unload) {
    e.getChunk.getBlockEntitiesPos.map(e.getChunk.getBlockEntity).foreach {
      case endpoint: WirelessEndpoint => remove(endpoint)
      case _ =>
    }
  }

  def add(endpoint: WirelessEndpoint) {
    dimensions.getOrElseUpdate(dimension(endpoint), new RTree[WirelessEndpoint](Settings.get.rTreeMaxEntries)((endpoint) => (endpoint.x + 0.5, endpoint.y + 0.5, endpoint.z + 0.5))).add(endpoint)
  }

  def update(endpoint: WirelessEndpoint) {
    dimensions.get(dimension(endpoint)) match {
      case Some(tree) =>
        tree(endpoint) match {
          case Some((x, y, z)) =>
            val dx = math.abs(endpoint.x + 0.5 - x)
            val dy = math.abs(endpoint.y + 0.5 - y)
            val dz = math.abs(endpoint.z + 0.5 - z)
            if (dx > 0.5 || dy > 0.5 || dz > 0.5) {
              tree.remove(endpoint)
              tree.add(endpoint)
            }
          case _ =>
        }
      case _ =>
    }
  }

  def remove(endpoint: WirelessEndpoint, dimension: RegistryKey[World]) = {
    dimensions.get(dimension) match {
      case Some(set) => set.remove(endpoint)
      case _ => false
    }
  }

  def remove(endpoint: WirelessEndpoint) = {
    dimensions.get(dimension(endpoint)) match {
      case Some(set) => set.remove(endpoint)
      case _ => false
    }
  }

  def computeReachableFrom(endpoint: WirelessEndpoint, strength: Double) = {
    dimensions.get(dimension(endpoint)) match {
      case Some(tree) if strength > 0 =>
        val range = strength + 1
        tree.query(offset(endpoint, -range), offset(endpoint, range)).
          filter(_ != endpoint).
          map(zipWithSquaredDistance(endpoint)).
          filter(_._2 <= range * range).
          map {
          case (c, distance) => (c, Math.sqrt(distance))
        } filter isUnobstructed(endpoint, strength) map (_._1)
      case _ => Iterable.empty[WirelessEndpoint]
    }
  }

  private def dimension(endpoint: WirelessEndpoint) = endpoint.world.dimension

  private def offset(endpoint: WirelessEndpoint, value: Double) =
    (endpoint.x + 0.5 + value, endpoint.y + 0.5 + value, endpoint.z + 0.5 + value)

  private def zipWithSquaredDistance(reference: WirelessEndpoint)(endpoint: WirelessEndpoint) =
    (endpoint, {
      val dx = endpoint.x - reference.x
      val dy = endpoint.y - reference.y
      val dz = endpoint.z - reference.z
      dx * dx + dy * dy + dz * dz
    })

  private def isUnobstructed(reference: WirelessEndpoint, strength: Double)(info: (WirelessEndpoint, Double)): Boolean = {
    val (endpoint, distance) = info
    val gap = distance - 1
    if (gap > 0) {
      // If there's some space between the two wireless network cards we try to
      // figure out if the signal might have been obstructed. We do this by
      // taking a few samples (more the further they are apart) and check if we
      // hit a block. For each block hit we subtract its hardness from the
      // surplus strength left after crossing the distance between the two. If
      // we reach a point where the surplus strength does not suffice we block
      // the message.
      val world = endpoint.world

      val origin = new Vector3d(reference.x, reference.y, reference.z)
      val target = new Vector3d(endpoint.x, endpoint.y, endpoint.z)

      // Vector from reference endpoint (sender) to this one (receiver).
      val delta = subtract(target, origin)
      val v = delta.normalize()

      // Get the vectors that are orthogonal to the direction vector.
      val up = if (v.x == 0 && v.z == 0) {
        assert(v.y != 0)
        new Vector3d(1, 0, 0)
      }
      else {
        new Vector3d(0, 1, 0)
      }
      val side = crossProduct(v, up)
      val top = crossProduct(v, side)

      // Accumulated obstructions and number of samples.
      var hardness = 0.0
      val samples = math.max(1, math.sqrt(gap).toInt)

      for (i <- 0 until samples) {
        val rGap = world.random.nextDouble() * gap
        // Adding some jitter to avoid only tracking the perfect line between
        // two endpoints when they are diagonal to each other for example.
        val rSide = world.random.nextInt(3) - 1
        val rTop = world.random.nextInt(3) - 1
        val x = (origin.x + v.x * rGap + side.x * rSide + top.x * rTop).toInt
        val y = (origin.y + v.y * rGap + side.y * rSide + top.y * rTop).toInt
        val z = (origin.z + v.z * rGap + side.z * rSide + top.z * rTop).toInt
        val blockPos = BlockPosition(x, y, z, world)
        if (world.isLoaded(blockPos)) Option(world.getBlock(blockPos)) match {
          case Some(block) => hardness += block.getBlockHardness(blockPos)
          case _ =>
        }
      }

      // Normalize and scale obstructions:
      hardness *= gap / samples

      // See if we have enough power to overcome the obstructions.
      strength - gap > hardness
    }
    else true
  }

  private def subtract(v1: Vector3d, v2: Vector3d) = new Vector3d(v1.x - v2.x, v1.y - v2.y, v1.z - v2.z)

  private def crossProduct(v1: Vector3d, v2: Vector3d) = new Vector3d(v1.y * v2.z - v1.z * v2.y, v1.z * v2.x - v1.x * v2.z, v1.x * v2.y - v1.y * v2.x)
}
