package li.cil.oc.integration.opencomputers

import java.util

import li.cil.oc.Constants
import li.cil.oc.api
import li.cil.oc.api.detail.ItemInfo
import li.cil.oc.api.driver.Converter
import li.cil.oc.server.component
import net.minecraft.item.ItemStack

import scala.collection.convert.ImplicitConversionsToScala._

object ConverterLinkedCard extends Converter {
  lazy val linkedCard: ItemInfo = api.Items.get(Constants.ItemName.LinkedCard)

  override def convert(value: scala.Any, output: util.Map[AnyRef, AnyRef]): Unit = value match {
    case stack: ItemStack if api.Items.get(stack) == linkedCard =>
      val card = new component.LinkedCard()
      output += "linkChannel" -> card.tunnel
    case _ => // Ignore.
  }
}
