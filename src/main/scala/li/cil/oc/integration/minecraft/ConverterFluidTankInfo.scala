package li.cil.oc.integration.minecraft

import java.util

import li.cil.oc.api
import net.minecraftforge.fluids

import scala.collection.convert.WrapAsScala._

object ConverterFluidTankInfo extends api.driver.Converter {
  override def convert(value: AnyRef, output: util.Map[AnyRef, AnyRef]) =
    value match {
      case tankInfo: fluids.FluidTankInfo =>
        output += "capacity" -> Int.box(tankInfo.capacity)
        if (tankInfo.fluid != null) {
          ConverterFluidStack.convert(tankInfo.fluid, output)
        }
        else output += "amount" -> Int.box(0)
      case _ =>
    }
}
