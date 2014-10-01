package li.cil.oc.server.driver.converter

import java.util

import li.cil.oc.{Settings, api}
import net.minecraft.item

import scala.collection.convert.WrapAsScala._

object ItemStack extends api.driver.Converter {
  override def convert(value: AnyRef, output: util.Map[AnyRef, AnyRef]) =
    value match {
      case stack: item.ItemStack =>
        if (Settings.get.insertIdsInConverters) {
          output += "id" -> Int.box(stack.itemID)
        }
        output += "damage" -> Int.box(stack.getItemDamage)
        output += "maxDamage" -> Int.box(stack.getMaxDamage)
        output += "size" -> Int.box(stack.stackSize)
        output += "maxSize" -> Int.box(stack.getMaxStackSize)
        output += "hasTag" -> Boolean.box(stack.hasTagCompound)
        output += "name" -> stack.getUnlocalizedName
        if (stack.hasDisplayName) {
          output += "label" -> stack.getDisplayName
        }
      case _ =>
    }
}
