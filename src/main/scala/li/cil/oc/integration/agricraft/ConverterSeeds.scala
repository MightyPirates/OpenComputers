package li.cil.oc.integration.agricraft

import java.util

import com.InfinityRaider.AgriCraft.api.v1.ISeedStats
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
              "gain" -> float2Float(stats.getGain),
              "maxGain" -> float2Float(stats.getMaxGain),
              "growth" -> float2Float(stats.getGrowth),
              "maxGrowth" -> float2Float(stats.getMaxGrowth),
              "strength" -> float2Float(stats.getStrength),
              "maxStrength" -> float2Float(stats.getMaxStrength)
            )
          case _ =>
        }
      })
      case _ =>
    }
  }
}
