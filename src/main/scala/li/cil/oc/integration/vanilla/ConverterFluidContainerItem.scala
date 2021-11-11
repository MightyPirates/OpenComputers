package li.cil.oc.integration.vanilla

import java.util

import li.cil.oc.{Settings, api}
import net.minecraft.item
import net.minecraft.item.ItemStack
import net.minecraftforge.fluids
import net.minecraftforge.fluids.FluidStack

import scala.collection.convert.WrapAsScala._

object ConverterFluidContainerItem extends api.driver.Converter  {
  override def convert(value: AnyRef, output: util.Map[AnyRef, AnyRef]): Unit =
    value match {
      case stack: item.ItemStack => stack.getItem match {
        case fc: fluids.IFluidContainerItem =>
          output += "capacity" -> Int.box(fc.getCapacity(stack))
          val fluidStack  = fc.getFluid(stack)
          if (fluidStack != null && fluidStack.getFluid != null) {
            val fluid = fluidStack.getFluid
            if (Settings.get.insertIdsInConverters)
              output += "fluid_id" -> Int.box(fluid.getID)
            output += "amount" -> Int.box(fluidStack.amount)
            output += "fluid_hasTag" -> Boolean.box(fluidStack.tag != null)
            if (fluid != null) {
              output += "fluid_name" -> fluid.getName
              output += "fluid_label" -> fluid.getLocalizedName(fluidStack)
            }
          }
          else
            output += "amount" -> Int.box(0)
        case _ =>
      }
      case _ =>
    }
}
