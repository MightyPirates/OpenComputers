package li.cil.oc.integration.agricraft

import java.util

import li.cil.oc.api.driver.Converter
import net.minecraft.item.ItemStack

import scala.collection.convert.WrapAsScala._

object ConverterSeeds extends Converter {
  override def convert(value: scala.Any, output: util.Map[AnyRef, AnyRef]): Unit = {
    value match {
      case stack: ItemStack => ApiHandler.Api.foreach(api => {
        if (api.isHandledByAgricraft(stack) && stack.hasTagCompound && stack.getTagCompound.getBoolean("analyzed")) api.getSeedStats(stack) match {
          case stats: ISeedStats =>
            output += "agricraft" -> Map(
              "gain" -> Float.box(stats.getGain),
              "maxGain" -> Float.box(stats.getMaxGain),
              "growth" -> Float.box(stats.getGrowth),
              "maxGrowth" -> Float.box(stats.getMaxGrowth),
              "strength" -> Float.box(stats.getStrength),
              "maxStrength" -> Float.box(stats.getMaxStrength)
            )
          case _ =>
        }
      })
      case _ =>
    }
  }
}
