package li.cil.oc.integration.agricraft

import com.InfinityRaider.AgriCraft.api.v1.ISeedStats
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import li.cil.oc.api.event.GeolyzerEvent

import scala.collection.convert.WrapAsScala._

object EventHandlerAgriCraft {
  @SubscribeEvent
  def onGeolyzerAnalyze(e: GeolyzerEvent.Analyze) {
    val world = e.host.world

    ApiHandler.Api.foreach(api => if (api.isCrops(world, e.x, e.y, e.z)) {
      e.data += "growth" -> float2Float(if (api.isMature(world, e.x, e.y, e.z)) 1f else 0f)

      if (api.isAnalyzed(world, e.x, e.y, e.z)) {
        api.getStats(world, e.x, e.y, e.z) match {
          case stats: ISeedStats =>
            e.data += "gain" -> float2Float(stats.getGain)
            e.data += "maxGain" -> float2Float(stats.getMaxGain)
            e.data += "growth" -> float2Float(stats.getGrowth)
            e.data += "maxGrowth" -> float2Float(stats.getMaxGrowth)
            e.data += "strength" -> float2Float(stats.getStrength)
            e.data += "maxStrength" -> float2Float(stats.getMaxStrength)
          case _ => // Invalid crop.
        }
      }
    })
  }
}
