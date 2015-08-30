package li.cil.oc.integration.vanilla

import li.cil.oc.api.event.GeolyzerEvent
import net.minecraft.block.BlockCrops
import net.minecraft.init.Blocks
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

import scala.collection.convert.WrapAsScala._

object EventHandlerVanilla {
  @SubscribeEvent
  def onGeolyzerAnalyze(e: GeolyzerEvent.Analyze) {
    val world = e.host.world
    val blockState = world.getBlockState(e.pos)
    val block = blockState.getBlock
    if (block.isInstanceOf[BlockCrops] || block == Blocks.melon_stem || block == Blocks.pumpkin_stem || block == Blocks.carrots || block == Blocks.potatoes) {
      e.data += "growth" -> float2Float((block.getMetaFromState(blockState) / 7f) max 0 min 1)
    }
    if (block == Blocks.cocoa) {
      e.data += "growth" -> float2Float(((block.getMetaFromState(blockState) >> 2) / 2f) max 0 min 1)
    }
    if (block == Blocks.nether_wart) {
      e.data += "growth" -> float2Float((block.getMetaFromState(blockState) / 3f) max 0 min 1)
    }
    if (block == Blocks.melon_block || block == Blocks.pumpkin || block == Blocks.cactus || block == Blocks.reeds) {
      e.data += "growth" -> float2Float(1f)
    }
  }
}
