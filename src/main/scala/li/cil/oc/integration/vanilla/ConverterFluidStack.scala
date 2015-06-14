package li.cil.oc.integration.vanilla

import java.util

import li.cil.oc.Settings
import li.cil.oc.api

import scala.collection.convert.WrapAsScala._

object ConverterFluidStack extends api.driver.Converter {
  override def convert(value: scala.Any, output: util.Map[AnyRef, AnyRef]) =
    value match {
      case stack: net.minecraftforge.fluids.FluidStack =>
        if (Settings.get.insertIdsInConverters) {
          output += "id" -> Int.box(stack.getFluid.getID)
        }
        output += "amount" -> Int.box(stack.amount)
        output += "hasTag" -> Boolean.box(stack.tag != null)
        val fluid = stack.getFluid
        if (fluid != null) {
          output += "name" -> fluid.getName
          output += "label" -> fluid.getLocalizedName(stack)
        }
      case _ =>
    }
}
