package li.cil.oc.integration.forestry

import java.util

import forestry.api.circuits.{ChipsetManager, ICircuit}
import forestry.api.genetics.AlleleManager
import li.cil.oc.api.driver.Converter
import net.minecraft.item.ItemStack

import scala.collection.convert.WrapAsScala._

object ConverterItemStack extends Converter {
  override def convert(value: scala.Any, output: util.Map[AnyRef, AnyRef]): Unit = value match {
    case stack: ItemStack if AlleleManager.alleleRegistry.isIndividual(stack) =>
      output += "individual" -> AlleleManager.alleleRegistry.getIndividual(stack)
    case stack: ItemStack if ChipsetManager.circuitRegistry.getCircuitboard(stack) != null => {
      val cc = ChipsetManager.circuitRegistry.getCircuitboard(stack).getCircuits
      val names = cc.collect{case c: ICircuit => c.getName}
      if (names.length > 0)
        output += "circuits" -> names
    }
    case _ =>
  }
}
