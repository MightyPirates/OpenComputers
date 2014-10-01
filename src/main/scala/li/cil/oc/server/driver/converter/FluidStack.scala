package li.cil.oc.server.driver.converter

import java.util

import li.cil.oc.{Settings, api}

import scala.collection.convert.WrapAsScala._

object FluidStack extends api.driver.Converter {
  override def convert(value: scala.Any, output: util.Map[AnyRef, AnyRef]) =
    value match {
      case stack: net.minecraftforge.fluids.FluidStack =>
        if (Settings.get.insertIdsInConverters) {
          output += "id" -> Int.box(stack.fluidID)
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
