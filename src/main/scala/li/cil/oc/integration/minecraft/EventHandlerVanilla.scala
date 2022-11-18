package li.cil.oc.integration.minecraft

import li.cil.oc.Settings
import li.cil.oc.api.event.GeolyzerEvent
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedWorld._
import net.minecraft.block.Block
import net.minecraft.block.CropsBlock
import net.minecraft.block.StemBlock
import net.minecraft.state.IntegerProperty
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fluids.IFluidBlock

import scala.collection.convert.ImplicitConversionsToScala._

object EventHandlerVanilla {
  @SubscribeEvent
  def onGeolyzerScan(e: GeolyzerEvent.Scan) {
    val world = e.host.world
    val blockPos = BlockPosition(e.host)
    val includeReplaceable = e.options.get("includeReplaceable") match {
      case value: java.lang.Boolean => value.booleanValue()
      case _ => true
    }

    val noise = new Array[Byte](e.data.length)
    world.random.nextBytes(noise)
    // Map to [-1, 1). The additional /33f is for normalization below.
    noise.map(_ / 128f / 33f).copyToArray(e.data)

    val w = e.maxX - e.minX + 1
    val d = e.maxZ - e.minZ + 1
    for (ry <- e.minY to e.maxY; rz <- e.minZ to e.maxZ; rx <- e.minX to e.maxX) {
      val pos = blockPos.toBlockPos.offset(rx, ry, rz)
      val index = (rx - e.minX) + ((rz - e.minZ) + (ry - e.minY) * d) * w
      if (world.isLoaded(pos) && !world.isEmptyBlock(pos)) {
        val blockState = world.getBlockState(pos)
        if (!blockState.getBlock.isAir(blockState, world, pos) && (includeReplaceable || blockState.getBlock.isInstanceOf[IFluidBlock] || !blockState.getMaterial.isReplaceable)) {
          val distance = math.sqrt(rx * rx + ry * ry + rz * rz).toFloat
          e.data(index) = e.data(index) * distance * Settings.get.geolyzerNoise + blockState.getDestroySpeed(world, pos)
        }
        else e.data(index) = 0
      }
      else e.data(index) = 0
    }
  }

  private def getGrowth(blockState: BlockState) = {
    blockState.getProperties().find(prop => {prop.isInstanceOf[IntegerProperty] && prop.getName() == "age"}) match {
      case Some(prop) => {
        val propAge = prop.asInstanceOf[IntegerProperty]
        Some((blockState.getValue(propAge).toFloat / propAge.getPossibleValues().max) max 0 min 1)
      }
      case None => None
    }
  }

  @SubscribeEvent
  def onGeolyzerAnalyze(e: GeolyzerEvent.Analyze) {
    val world = e.host.world
    val blockState = world.getBlockState(e.pos)
    val block = blockState.getBlock

    e.data += "name" -> block.getRegistryName
    e.data += "hardness" -> Float.box(blockState.getDestroySpeed(world, e.pos))
    e.data += "harvestLevel" -> Int.box(block.getHarvestLevel(blockState))
    e.data += "harvestTool" -> Option(block.getHarvestTool(blockState)).map(_.getName).orNull
    e.data += "color" -> Int.box(blockState.getMapColor(world, e.pos).col)

    // backward compatibility
    e.data += "metadata" -> Int.box(0)

    e.data += "properties" -> {
      var props:Map[String, Any] = Map();
      for (prop <- blockState.getProperties()) {
        props += prop.getName() -> blockState.getValue(prop)
      }
      props
    }

    if (Settings.get.insertIdsInConverters) {
      e.data += "id" -> Int.box(Block.getId(blockState))
    }

    {
      if (block.isInstanceOf[CropsBlock] || block.isInstanceOf[StemBlock] || block == Blocks.COCOA || block == Blocks.NETHER_WART || block == Blocks.CHORUS_FLOWER) {
        getGrowth(blockState)
      } else if (block == Blocks.MELON || block == Blocks.PUMPKIN || block == Blocks.CACTUS || block == Blocks.SUGAR_CANE || block == Blocks.CHORUS_PLANT) {
        Some(1f)
      } else {
        None
      }
    } foreach { growth =>
      e.data += "growth" -> Float.box(growth)
    }
  }
}
