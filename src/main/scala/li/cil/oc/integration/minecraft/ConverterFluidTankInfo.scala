package li.cil.oc.integration.minecraft

import java.util

import li.cil.oc.api
import net.minecraftforge.fluids

import scala.collection.convert.WrapAsScala._

object ConverterFluidTankInfo extends api.driver.Converter {
  override def convert(value: AnyRef, output: util.Map[AnyRef, AnyRef]) =
    value match {
      case tankInfo: fluids.IFluidTank =>
        output += "capacity" -> Int.box(tankInfo.getCapacity)
        if (!tankInfo.getFluid.isEmpty) {
          ConverterFluidStack.convert(tankInfo.getFluid, output)
        }
        else output += "amount" -> Int.box(0)
      case _ =>
    }
}
