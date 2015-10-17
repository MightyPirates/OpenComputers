package li.cil.oc.common.event

import cpw.mods.fml.common.eventhandler.SubscribeEvent
import li.cil.oc.Settings
import li.cil.oc.api.event.GeolyzerEvent
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedWorld._
import net.minecraft.block.Block
import net.minecraftforge.fluids.FluidRegistry

import scala.collection.convert.WrapAsScala._

object GeolyzerHandler {
  @SubscribeEvent
  def onGeolyzerScan(e: GeolyzerEvent.Scan) {
    val world = e.host.world
    val blockPos = BlockPosition(e.host)
    val bx = blockPos.x + e.scanX
    val bz = blockPos.z + e.scanZ
    val includeReplaceable = e.options.get("includeReplaceable") match {
      case value: java.lang.Boolean => value.booleanValue()
      case _ => true
    }

    val noise = new Array[Byte](e.data.length)
    world.rand.nextBytes(noise)
    // Map to [-1, 1). The additional /33f is for normalization below.
    noise.map(_ / 128f / 33f).copyToArray(e.data)

    for (ry <- 0 until e.data.length) {
      val by = blockPos.y + ry - 32
      if (!world.isAirBlock(bx, by, bz)) {
        val block = world.getBlock(bx, by, bz)
        if (block != null && (includeReplaceable || isFluid(block) || !block.isReplaceable(world, blockPos.x, blockPos.y, blockPos.z))) {
          e.data(ry) = e.data(ry) * (math.abs(ry - 32) + 1) * Settings.get.geolyzerNoise + block.getBlockHardness(world, bx, by, bz)
        }
        else e.data(ry) = 0
      }
      else e.data(ry) = 0
    }
  }

  @SubscribeEvent
  def onGeolyzerAnalyze(e: GeolyzerEvent.Analyze) {
    val world = e.host.world
    val blockPos = BlockPosition(e.x, e.y, e.z, world)
    val block = world.getBlock(blockPos)

    e.data += "name" -> Block.blockRegistry.getNameForObject(block)
    e.data += "metadata" -> Int.box(world.getBlockMetadata(blockPos))
    e.data += "hardness" -> Float.box(world.getBlockHardness(blockPos))
    e.data += "harvestLevel" -> Int.box(world.getBlockHarvestLevel(blockPos))
    e.data += "harvestTool" -> world.getBlockHarvestTool(blockPos)
    e.data += "color" -> Int.box(world.getBlockMapColor(blockPos).colorValue)

    if (Settings.get.insertIdsInConverters)
      e.data += "id" -> Int.box(Block.getIdFromBlock(block))
  }

  private def isFluid(block: Block) = FluidRegistry.lookupFluidForBlock(block) != null
}
