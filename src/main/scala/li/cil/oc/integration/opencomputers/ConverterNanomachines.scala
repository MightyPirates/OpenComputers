package li.cil.oc.integration.opencomputers

import java.util

import com.google.common.base.Strings
import li.cil.oc.Constants
import li.cil.oc.api
import li.cil.oc.api.driver.Converter
import li.cil.oc.common.item.data.NanomachineData
import net.minecraft.item.ItemStack

import scala.collection.convert.ImplicitConversionsToScala._

object ConverterNanomachines extends Converter {
  lazy val nanomachines = api.Items.get(Constants.ItemName.Nanomachines)

  override def convert(value: scala.Any, output: util.Map[AnyRef, AnyRef]): Unit = value match {
    case stack: ItemStack if api.Items.get(stack) == nanomachines =>
      val data = new NanomachineData(stack)
      if (!Strings.isNullOrEmpty(data.uuid)) {
        output += "nanomachines" -> data.uuid
      }
    case _ => // Ignore.
  }
}
