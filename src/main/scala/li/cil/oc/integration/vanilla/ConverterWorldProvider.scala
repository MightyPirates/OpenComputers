package li.cil.oc.integration.vanilla

import java.util

import li.cil.oc.api
import net.minecraft.world

import scala.collection.convert.WrapAsScala._

object ConverterWorldProvider extends api.driver.Converter {
  override def convert(value: AnyRef, output: util.Map[AnyRef, AnyRef]) =
    value match {
      case provider: world.WorldProvider =>
        output += "id" -> Int.box(provider.dimensionId)
        output += "name" -> provider.getDimensionName
      case _ =>
    }
}
