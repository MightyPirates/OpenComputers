package li.cil.oc.integration.thaumcraft

import java.util

import li.cil.oc.api.driver.Converter
import net.minecraft.item.ItemStack
import thaumcraft.api.aspects.AspectList

import scala.collection.convert.WrapAsScala._

object ConverterAspectItem extends Converter {
  override def convert(value: scala.Any, output: util.Map[AnyRef, AnyRef]): Unit = value match {
    case stack: ItemStack if stack.hasTagCompound =>
      val aspects = new AspectList()
      aspects.readFromNBT(stack.getTagCompound)
      output += "aspects" -> aspects
    case _ =>
  }
}
