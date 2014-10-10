package li.cil.oc.integration.vanilla

import java.util

import li.cil.oc.Settings
import li.cil.oc.api
import net.minecraft.item
import net.minecraft.item.Item

import scala.collection.convert.WrapAsScala._

object ConverterItemStack extends api.driver.Converter {
  override def convert(value: AnyRef, output: util.Map[AnyRef, AnyRef]) =
    value match {
      case stack: item.ItemStack =>
        if (Settings.get.insertIdsInConverters) {
          output += "id" -> Int.box(Item.getIdFromItem(stack.getItem))
        }
        output += "damage" -> Int.box(stack.getItemDamage)
        output += "maxDamage" -> Int.box(stack.getMaxDamage)
        output += "size" -> Int.box(stack.stackSize)
        output += "maxSize" -> Int.box(stack.getMaxStackSize)
        output += "hasTag" -> Boolean.box(stack.hasTagCompound)
        output += "name" -> Item.itemRegistry.getNameForObject(stack.getItem)
        output += "label" -> stack.getDisplayName
      case _ =>
    }
}
