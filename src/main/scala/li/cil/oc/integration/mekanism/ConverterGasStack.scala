package li.cil.oc.integration.mekanism

import java.util

import li.cil.oc.Settings
import li.cil.oc.api

import scala.collection.convert.WrapAsScala._

object ConverterGasStack extends api.driver.Converter {
  override def convert(value: scala.Any, output: util.Map[AnyRef, AnyRef]) =
    value match {
      case stack: mekanism.api.gas.GasStack =>
        if (Settings.get.insertIdsInConverters) {
          output += "id" -> Int.box(stack.getGas.getID)
        }
        output += "amount" -> Int.box(stack.amount)
        val gas = stack.getGas
        if (gas != null) {
          output += "name" -> gas.getName
          output += "label" -> gas.getLocalizedName
        }
      case _ =>
    }
}
