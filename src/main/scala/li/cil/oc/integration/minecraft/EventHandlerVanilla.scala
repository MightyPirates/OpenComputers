package li.cil.oc.integration.minecraft

import li.cil.oc.Settings
import li.cil.oc.api.event.GeolyzerEvent
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedWorld._
import net.minecraft.block.Block
import net.minecraft.block.BlockCrops
import net.minecraft.init.Blocks
import net.minecraftforge.fluids.FluidRegistry
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

import scala.collection.convert.WrapAsScala._

object EventHandlerVanilla {
  @SubscribeEvent
  def onGeolyzerScan(e: GeolyzerEvent.Scan) {
    val world = e.host.getWorld
    val blockPos = BlockPosition(e.host)
    val includeReplaceable = e.options.get("includeReplaceable") match {
      case value: java.lang.Boolean => value.booleanValue()
      case _ => true
    }

    val noise = new Array[Byte](e.data.length)
    world.rand.nextBytes(noise)
    // Map to [-1, 1). The additional /33f is for normalization below.
    noise.map(_ / 128f / 33f).copyToArray(e.data)

    val w = e.maxX - e.minX + 1
    val d = e.maxZ - e.minZ + 1
    for (ry <- e.minY to e.maxY; rz <- e.minZ to e.maxZ; rx <- e.minX to e.maxX) {
      val pos = blockPos.toBlockPos.add(rx, ry, rz)
      val x = blockPos.x + rx
      val y = blockPos.y + ry
      val z = blockPos.z + rz
      val index = (rx - e.minX) + ((rz - e.minZ) + (ry - e.minY) * d) * w
      if (world.isBlockLoaded(pos) && !world.isAirBlock(pos)) {
        val block = world.getBlockState(pos).getBlock
        if (block != Blocks.AIR && (includeReplaceable || isFluid(block) || !block.isReplaceable(world, blockPos.toBlockPos))) {
          val distance = math.sqrt(rx * rx + ry * ry + rz * rz).toFloat
          e.data(index) = e.data(index) * distance * Settings.Misc.geolyzerNoise + block.getBlockHardness(world.getBlockState(pos), world, pos)
        }
        else e.data(index) = 0
      }
      else e.data(index) = 0
    }
  }

  private def isFluid(block: Block) = FluidRegistry.lookupFluidForBlock(block) != null

  @SubscribeEvent
  def onGeolyzerAnalyze(e: GeolyzerEvent.Analyze) {
    val world = e.host.getWorld
    val blockState = world.getBlockState(e.pos)
    val block = blockState.getBlock

    e.data += "name" -> Block.REGISTRY.getNameForObject(block)
    e.data += "hardness" -> Float.box(block.getBlockHardness(world.getBlockState(e.pos), world, e.pos))
    e.data += "harvestLevel" -> Int.box(block.getHarvestLevel(blockState))
    e.data += "harvestTool" -> block.getHarvestTool(blockState)
    e.data += "color" -> Int.box(block.getMapColor(blockState).colorValue)

    if (Settings.Debug.insertIdsInConverters) {
      e.data += "id" -> Int.box(Block.getIdFromBlock(block))
    }

    if (block.isInstanceOf[BlockCrops] || block == Blocks.MELON_STEM || block == Blocks.PUMPKIN_STEM || block == Blocks.CARROTS || block == Blocks.POTATOES) {
      e.data += "growth" -> Float.box((block.getMetaFromState(blockState) / 7f) max 0 min 1)
    }
    if (block == Blocks.COCOA) {
      e.data += "growth" -> Float.box(((block.getMetaFromState(blockState) >> 2) / 2f) max 0 min 1)
    }
    if (block == Blocks.NETHER_WART) {
      e.data += "growth" -> Float.box((block.getMetaFromState(blockState) / 3f) max 0 min 1)
    }
    if (block == Blocks.MELON_BLOCK || block == Blocks.PUMPKIN || block == Blocks.CACTUS || block == Blocks.REEDS) {
      e.data += "growth" -> Float.box(1f)
    }
  }
}
