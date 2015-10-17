package li.cil.oc.integration.agricraft

import li.cil.oc.api.event.GeolyzerEvent

import scala.collection.convert.WrapAsScala._

object EventHandlerAgriCraft {
  @SubscribeEvent
  def onGeolyzerAnalyze(e: GeolyzerEvent.Analyze) {
    val world = e.host.world

    ApiHandler.Api.foreach(api => if (api.isCrops(world, e.x, e.y, e.z)) {
      e.data += "growth" -> Float.box(if (api.isMature(world, e.x, e.y, e.z)) 1f else 0f)

      if (api.isAnalyzed(world, e.x, e.y, e.z)) {
        api.getStats(world, e.x, e.y, e.z) match {
          case stats: ISeedStats =>
            e.data += "gain" -> Float.box(stats.getGain)
            e.data += "maxGain" -> Float.box(stats.getMaxGain)
            e.data += "growth" -> Float.box(stats.getGrowth)
            e.data += "maxGrowth" -> Float.box(stats.getMaxGrowth)
            e.data += "strength" -> Float.box(stats.getStrength)
            e.data += "maxStrength" -> Float.box(stats.getMaxStrength)
          case _ => // Invalid crop.
        }
      }
    })
  }
}
