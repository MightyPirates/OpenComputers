package li.cil.oc.integration.minecraft

import java.util

import li.cil.oc.api

import scala.collection.convert.ImplicitConversionsToScala._

object ConverterFluidStack extends api.driver.Converter {
  override def convert(value: scala.Any, output: util.Map[AnyRef, AnyRef]) =
    value match {
      case stack: net.minecraftforge.fluids.FluidStack =>
        output += "amount" -> Int.box(stack.getAmount)
        output += "hasTag" -> Boolean.box(stack.hasTag)
        val fluid = stack.getFluid
        output += "name" -> fluid.getRegistryName.toString
        output += "label" -> fluid.getAttributes.getDisplayName(stack).getString
      case _ =>
    }
}
