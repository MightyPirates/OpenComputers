package li.cil.oc.integration.util

import li.cil.oc.integration.Mods
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedWorld._
import net.minecraft.util.Direction

import scala.collection.mutable

object BundledRedstone {
  val providers = mutable.Buffer.empty[RedstoneProvider]

  def addProvider(provider: RedstoneProvider): Unit = providers += provider

  def isAvailable = providers.nonEmpty

  def computeInput(pos: BlockPosition, side: Direction): Int = {
    if (pos.world.get.blockExists(pos.offset(side)))
      providers.map(_.computeInput(pos, side)).padTo(1, 0).max
    else 0
  }

  def computeBundledInput(pos: BlockPosition, side: Direction): Array[Int] = {
    if (pos.world.get.blockExists(pos.offset(side))) {
      val inputs = providers.map(_.computeBundledInput(pos, side)).filter(_ != null)
      if (inputs.isEmpty) null
      else inputs.reduce((a, b) => a.lazyZip(b).map((l, r) => math.max(l, r)))
    }
    else null
  }

  trait RedstoneProvider {
    def computeInput(pos: BlockPosition, side: Direction): Int

    def computeBundledInput(pos: BlockPosition, side: Direction): Array[Int]
  }

}
