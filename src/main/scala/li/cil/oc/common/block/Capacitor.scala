package li.cil.oc.common.block

import java.util.Random

import li.cil.oc.common.tileentity
import li.cil.oc.integration.Mods
import li.cil.oc.integration.coloredlights.ModColoredLights
import net.minecraft.block.Block
import net.minecraft.world.World

class Capacitor extends SimpleBlock {
  ModColoredLights.setLightLevel(this, 5, 5, 5)

  setTickRandomly(true)

  override protected def customTextures = Array(
    None,
    Some("CapacitorTop"),
    Some("CapacitorSide"),
    Some("CapacitorSide"),
    Some("CapacitorSide"),
    Some("CapacitorSide")
  )

  // ----------------------------------------------------------------------- //

  override def hasTileEntity(metadata: Int) = true

  override def createTileEntity(world: World, metadata: Int) = new tileentity.Capacitor()

  // ----------------------------------------------------------------------- //

  override def hasComparatorInputOverride = true

  override def getComparatorInputOverride(world: World, x: Int, y: Int, z: Int, side: Int) =
    world.getTileEntity(x, y, z) match {
      case capacitor: tileentity.Capacitor if !world.isRemote =>
        math.round(15 * capacitor.node.localBuffer / capacitor.node.localBufferSize).toInt
      case _ => 0
    }

  override def updateTick(world: World, x: Int, y: Int, z: Int, rng: Random): Unit = {
    world.notifyBlocksOfNeighborChange(x, y, z, this)
  }

  override def tickRate(world : World) = 1

  override def onNeighborBlockChange(world: World, x: Int, y: Int, z: Int, block: Block) =
    world.getTileEntity(x, y, z) match {
      case capacitor: tileentity.Capacitor => capacitor.recomputeCapacity()
      case _ =>
    }
}
