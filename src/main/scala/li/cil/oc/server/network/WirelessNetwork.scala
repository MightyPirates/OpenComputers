package li.cil.oc.server.network

import cpw.mods.fml.common.eventhandler.SubscribeEvent
import li.cil.oc.Settings
import li.cil.oc.api.network.WirelessEndpoint
import li.cil.oc.util.RTree
import net.minecraft.util.Vec3
import net.minecraftforge.event.world.ChunkEvent
import net.minecraftforge.event.world.WorldEvent

import scala.collection.convert.WrapAsScala._
import scala.collection.mutable

object WirelessNetwork {
  val dimensions = mutable.Map.empty[Int, RTree[WirelessEndpoint]]

  @SubscribeEvent
  def onWorldUnload(e: WorldEvent.Unload) {
    if (!e.world.isRemote) {
      dimensions.remove(e.world.provider.dimensionId)
    }
  }

  @SubscribeEvent
  def onWorldLoad(e: WorldEvent.Load) {
    if (!e.world.isRemote) {
      dimensions.remove(e.world.provider.dimensionId)
    }
  }

  // Safety clean up, in case some tile entities didn't properly leave the net.
  @SubscribeEvent
  def onChunkUnload(e: ChunkEvent.Unload) {
    e.getChunk.chunkTileEntityMap.values.foreach {
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

  def remove(endpoint: WirelessEndpoint, dimension: Int) = {
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
        } filter isUnobstructed(endpoint, strength) map(_._1)
      case _ => Iterable.empty[WirelessEndpoint]
    }
  }

  private def dimension(endpoint: WirelessEndpoint) = endpoint.world.provider.dimensionId

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

      val origin = Vec3.createVectorHelper(reference.x, reference.y, reference.z)
      val target = Vec3.createVectorHelper(endpoint.x, endpoint.y, endpoint.z)

      // Vector from reference endpoint (sender) to this one (receiver).
      val delta = subtract(target, origin)
      val v = delta.normalize()

      // Get the vectors that are orthogonal to the direction vector.
      val up = if (v.xCoord == 0 && v.zCoord == 0) {
        assert(v.yCoord != 0)
        Vec3.createVectorHelper(1, 0, 0)
      }
      else {
        Vec3.createVectorHelper(0, 1, 0)
      }
      val side = crossProduct(v, up)
      val top = crossProduct(v, side)

      // Accumulated obstructions and number of samples.
      var hardness = 0.0
      val samples = math.max(1, math.sqrt(gap).toInt)

      for (i <- 0 until samples) {
        val rGap = world.rand.nextDouble() * gap
        // Adding some jitter to avoid only tracking the perfect line between
        // two endpoints when they are diagonal to each other for example.
        val rSide = world.rand.nextInt(3) - 1
        val rTop = world.rand.nextInt(3) - 1
        val x = (origin.xCoord + v.xCoord * rGap + side.xCoord * rSide + top.xCoord * rTop).toInt
        val y = (origin.yCoord + v.yCoord * rGap + side.yCoord * rSide + top.yCoord * rTop).toInt
        val z = (origin.zCoord + v.zCoord * rGap + side.zCoord * rSide + top.zCoord * rTop).toInt
        if (world.blockExists(x, y, z)) Option(world.getBlock(x, y, z)) match {
          case Some(block) => hardness += block.getBlockHardness(world, x, y, z)
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

  private def subtract(v1: Vec3, v2: Vec3) = Vec3.createVectorHelper(v1.xCoord - v2.xCoord, v1.yCoord - v2.yCoord, v1.zCoord - v2.zCoord)

  private def crossProduct(v1: Vec3, v2: Vec3) = Vec3.createVectorHelper(v1.yCoord * v2.zCoord - v1.zCoord * v2.yCoord, v1.zCoord * v2.xCoord - v1.xCoord * v2.zCoord, v1.xCoord * v2.yCoord - v1.yCoord * v2.xCoord)
}
