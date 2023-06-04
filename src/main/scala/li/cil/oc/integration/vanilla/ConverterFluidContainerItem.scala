package li.cil.oc.integration.vanilla

import li.cil.oc.server.driver.Registry
import li.cil.oc.{Settings, api}
import net.minecraft.item.ItemStack
import net.minecraftforge.fluids.IFluidContainerItem

import java.util
import scala.collection.convert.WrapAsScala._

object ConverterFluidContainerItem extends api.driver.Converter {
  override def convert(value: scala.Any, output: util.Map[AnyRef, AnyRef]) =
    value match {
      case stack: ItemStack => stack.getItem match {
        case fc: IFluidContainerItem =>
          output += "capacity" -> Int.box(fc.getCapacity(stack))
          val fluidStack = fc.getFluid(stack)
          if (fluidStack != null) {
            val fluidData = Registry.convert(Array[AnyRef](fluidStack))
            if (fluidData.nonEmpty) {
              output += "fluid" -> fluidData(0)
            }
          }
          if (!output.containsKey("fluid")) {
            val fluidMap = new util.HashMap[AnyRef, AnyRef]()
            fluidMap += "amount" -> Int.box(0)
            output += "fluid" -> fluidMap
          }
        case _ =>
      }
      case _ =>
    }
}
