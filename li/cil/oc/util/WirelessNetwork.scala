package li.cil.oc.util

import li.cil.oc.Settings
import li.cil.oc.server.component.WirelessNetworkCard
import net.minecraft.block.Block
import net.minecraft.util.Vec3
import net.minecraftforge.event.ForgeSubscribe
import net.minecraftforge.event.world.WorldEvent
import scala.collection.mutable

object WirelessNetwork {
  val dimensions = mutable.Map.empty[Int, RTree[WirelessNetworkCard]]

  @ForgeSubscribe
  def onWorldUnload(e: WorldEvent.Unload) {
    if (!e.world.isRemote) {
      dimensions.remove(e.world.provider.dimensionId)
    }
  }

  @ForgeSubscribe
  def onWorldLoad(e: WorldEvent.Load) {
    if (!e.world.isRemote) {
      dimensions.remove(e.world.provider.dimensionId)
    }
  }

  def add(card: WirelessNetworkCard) {
    dimensions.getOrElseUpdate(dimension(card), new RTree[WirelessNetworkCard](Settings.get.rTreeMaxEntries)((card) => (card.owner.xCoord + 0.5, card.owner.yCoord + 0.5, card.owner.zCoord + 0.5))).add(card)
  }

  def update(card: WirelessNetworkCard) {
    dimensions.get(dimension(card)) match {
      case Some(tree) =>
        tree(card) match {
          case Some((x, y, z)) =>
            val dx = math.abs(card.owner.xCoord + 0.5 - x)
            val dy = math.abs(card.owner.yCoord + 0.5 - y)
            val dz = math.abs(card.owner.zCoord + 0.5 - z)
            if (dx > 0.5 || dy > 0.5 || dz > 0.5) {
              tree.remove(card)
              tree.add(card)
            }
          case _ =>
        }
      case _ =>
    }
  }

  def remove(card: WirelessNetworkCard) = {
    dimensions.get(dimension(card)) match {
      case Some(set) => set.remove(card)
      case _ => false
    }
  }

  def computeReachableFrom(card: WirelessNetworkCard) = {
    dimensions.get(dimension(card)) match {
      case Some(tree) if card.strength > 0 =>
        val range = card.strength + 1
        tree.query(offset(card, -range), offset(card, range)).
          filter(_ != card).
          map(zipWithDistance(card)).
          filter(_._2 <= range * range).
          map {
          case (c, distance) => (c, Math.sqrt(distance))
        }.
          filter(isUnobstructed(card))
      case _ => Iterable.empty[(WirelessNetworkCard, Double)] // Should not be possible.
    }
  }

  private def dimension(card: WirelessNetworkCard) = card.owner.worldObj.provider.dimensionId

  private def offset(card: WirelessNetworkCard, value: Double) =
    (card.owner.xCoord + 0.5 + value, card.owner.yCoord + 0.5 + value, card.owner.zCoord + 0.5 + value)

  private def zipWithDistance(reference: WirelessNetworkCard)(card: WirelessNetworkCard) =
    (card, card.owner.getDistanceFrom(
      reference.owner.xCoord + 0.5,
      reference.owner.yCoord + 0.5,
      reference.owner.zCoord + 0.5))

  private def isUnobstructed(reference: WirelessNetworkCard)(info: (WirelessNetworkCard, Double)): Boolean = {
    val (card, distance) = info
    val gap = distance - 1
    if (gap > 0) {
      // If there's some space between the two wireless network cards we try to
      // figure out if the signal might have been obstructed. We do this by
      // taking a few samples (more the further they are apart) and check if we
      // hit a block. For each block hit we subtract its hardness from the
      // surplus strength left after crossing the distance between the two. If
      // we reach a point where the surplus strength does not suffice we block
      // the message.
      val world = card.owner.worldObj
      val pool = world.getWorldVec3Pool

      val origin = pool.getVecFromPool(reference.owner.xCoord, reference.owner.yCoord, reference.owner.zCoord)
      val target = pool.getVecFromPool(card.owner.xCoord, card.owner.yCoord, card.owner.zCoord)

      // Vector from reference card (sender) to this one (receiver).
      val delta = subtract(target, origin)
      val v = delta.normalize()

      // Get the vectors that are orthogonal to the direction vector.
      val up = if (v.xCoord == 0 && v.zCoord == 0) {
        assert(v.yCoord != 0)
        pool.getVecFromPool(1, 0, 0)
      }
      else {
        pool.getVecFromPool(0, 1, 0)
      }
      val side = crossProduct(v, up)
      val top = crossProduct(v, side)

      // Accumulated obstructions and number of samples.
      //val delta = v.lengthVector
      var hardness = 0.0
      val samples = math.max(1, math.sqrt(gap).toInt)

      for (i <- 0 until samples) {
        val rGap = world.rand.nextDouble() * gap
        // Adding some jitter to avoid only tracking the perfect line between
        // two modems when they are diagonal to each other for example.
        val rSide = world.rand.nextInt(3) - 1
        val rTop = world.rand.nextInt(3) - 1
        val x = (origin.xCoord + v.xCoord * rGap + side.xCoord * rSide + top.xCoord * rTop).toInt
        val y = (origin.yCoord + v.yCoord * rGap + side.yCoord * rSide + top.yCoord * rTop).toInt
        val z = (origin.zCoord + v.zCoord * rGap + side.zCoord * rSide + top.zCoord * rTop).toInt
        Option(Block.blocksList(world.getBlockId(x, y, z))) match {
          case Some(block) => hardness += block.blockHardness
          case _ =>
        }
      }

      // Normalize and scale obstructions:
      hardness *= gap / samples

      // Remaining signal strength.
      val strength = reference.strength - gap

      // See if we have enough power to overcome the obstructions.
      strength > hardness
    }
    else true
  }

  private def subtract(v1: Vec3, v2: Vec3) = v1.myVec3LocalPool.getVecFromPool(v1.xCoord - v2.xCoord, v1.yCoord - v2.yCoord, v1.zCoord - v2.zCoord)

  private def crossProduct(v1: Vec3, v2: Vec3) = v1.myVec3LocalPool.getVecFromPool(v1.yCoord * v2.zCoord - v1.zCoord * v2.yCoord, v1.zCoord * v2.xCoord - v1.xCoord * v2.zCoord, v1.xCoord * v2.yCoord - v1.yCoord * v2.xCoord)
}
