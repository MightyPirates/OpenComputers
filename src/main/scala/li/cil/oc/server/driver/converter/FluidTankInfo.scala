package li.cil.oc.server.driver.converter

import java.util

import li.cil.oc.api
import net.minecraftforge.fluids

import scala.collection.convert.WrapAsScala._

object FluidTankInfo extends api.driver.Converter {
  override def convert(value: AnyRef, output: util.Map[AnyRef, AnyRef]) =
    value match {
      case tankInfo: fluids.FluidTankInfo =>
        output += "capacity" -> Int.box(tankInfo.capacity)
        if (tankInfo.fluid != null) {
          output += "amount" -> Int.box(tankInfo.fluid.amount)
          output += "id" -> Int.box(tankInfo.fluid.fluidID)
          val fluid = tankInfo.fluid.getFluid
          if (fluid != null) {
            output += "name" -> fluid.getName
            output += "label" -> fluid.getLocalizedName
          }
        }
        else output += "amount" -> Int.box(0)
      case _ =>
    }
}
