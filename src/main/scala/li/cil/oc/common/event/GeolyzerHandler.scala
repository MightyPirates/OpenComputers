package li.cil.oc.common.event

import cpw.mods.fml.common.eventhandler.SubscribeEvent
import li.cil.oc.Settings
import li.cil.oc.api.event.GeolyzerEvent
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedWorld._
import net.minecraft.block.Block
import net.minecraftforge.fluids.FluidRegistry

import scala.collection.convert.WrapAsScala._

// TODO move to EventHandlerVanilla
object GeolyzerHandler {
  @SubscribeEvent
  def onGeolyzerScan(e: GeolyzerEvent.Scan) {
    val world = e.host.world
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
      val x = blockPos.x + rx
      val y = blockPos.y + ry
      val z = blockPos.z + rz
      val index = (rx - e.minX) + ((rz - e.minZ) + (ry - e.minY) * d) * w
      if (world.blockExists(x, y, z) && !world.isAirBlock(x, y, z)) {
        val block = world.getBlock(x, y, z)
        if (block != null && (includeReplaceable || isFluid(block) || !block.isReplaceable(world, blockPos.x, blockPos.y, blockPos.z))) {
          val dx = blockPos.x - x
          val dy = blockPos.y - y
          val dz = blockPos.z - z
          val distance = math.sqrt(dx * dx + dy * dy + dz * dz).toFloat
          e.data(index) = e.data(index) * distance * Settings.get.geolyzerNoise + block.getBlockHardness(world, x, y, z)
        }
        else e.data(index) = 0
      }
      else e.data(index) = 0
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
