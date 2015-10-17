package li.cil.oc.integration.vanilla

import cpw.mods.fml.common.eventhandler.SubscribeEvent
import li.cil.oc.api.event.GeolyzerEvent
import net.minecraft.block.BlockCrops
import net.minecraft.init.Blocks

import scala.collection.convert.WrapAsScala._

object EventHandlerVanilla {
  @SubscribeEvent
  def onGeolyzerAnalyze(e: GeolyzerEvent.Analyze) {
    val world = e.host.world
    val block = world.getBlock(e.x, e.y, e.z)
    if (block.isInstanceOf[BlockCrops] || block == Blocks.melon_stem || block == Blocks.pumpkin_stem || block == Blocks.carrots || block == Blocks.potatoes) {
      e.data += "growth" -> Float.box((world.getBlockMetadata(e.x, e.y, e.z) / 7f) max 0 min 1)
    }
    if (block == Blocks.cocoa) {
      e.data += "growth" -> Float.box(((world.getBlockMetadata(e.x, e.y, e.z) >> 2) / 2f) max 0 min 1)
    }
    if (block == Blocks.nether_wart) {
      e.data += "growth" -> Float.box((world.getBlockMetadata(e.x, e.y, e.z) / 3f) max 0 min 1)
    }
    if (block == Blocks.melon_block || block == Blocks.pumpkin || block == Blocks.cactus || block == Blocks.reeds) {
      e.data += "growth" -> Float.box(1f)
    }
  }
}
