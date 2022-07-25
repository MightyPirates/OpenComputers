package li.cil.oc.integration.minecraft

import java.util

import li.cil.oc.api
import li.cil.oc.util.ExtendedArguments.TankProperties

import scala.collection.convert.WrapAsScala._

object ConverterFluidTankProperties extends api.driver.Converter {
  override def convert(value: AnyRef, output: util.Map[AnyRef, AnyRef]) =
    value match {
      case properties: TankProperties =>
        output += "capacity" -> Int.box(properties.capacity)
        val fluid = properties.contents
        if (fluid != null) {
          ConverterFluidStack.convert(fluid, output)
        }
        else output += "amount" -> Int.box(0)
      case _ =>
    }
}
