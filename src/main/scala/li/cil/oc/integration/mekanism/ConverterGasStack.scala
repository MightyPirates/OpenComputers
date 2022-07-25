package li.cil.oc.integration.mekanism

import java.util

import li.cil.oc.Settings
import li.cil.oc.api
import mekanism.api.MekanismAPI
import mekanism.api.chemical.gas.Gas
import mekanism.api.chemical.gas.GasStack
import net.minecraftforge.registries.ForgeRegistry

import scala.collection.convert.WrapAsScala._

object ConverterGasStack extends api.driver.Converter {
  override def convert(value: scala.Any, output: util.Map[AnyRef, AnyRef]) =
    value match {
      case stack: GasStack =>
        if (Settings.get.insertIdsInConverters) {
          output += "id" -> Int.box(MekanismAPI.gasRegistry().asInstanceOf[ForgeRegistry[Gas]].getID(stack.getType))
        }
        output += "amount" -> Long.box(stack.getAmount)
        val gas = stack.getType
        if (gas != null) {
          output += "name" -> gas.getRegistryName.toString
          output += "label" -> gas.getTextComponent.getString
        }
      case _ =>
    }
}
