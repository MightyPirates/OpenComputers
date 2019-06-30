package li.cil.oc.integration.thaumcraft

import java.util

import li.cil.oc.api.driver.Converter
import net.minecraft.item.{Item, ItemStack}
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.Constants.NBT

import scala.collection.convert.WrapAsScala._
import scala.collection.mutable

object ConverterThaumcraftItems extends Converter {
  override def convert(value: scala.Any, output: util.Map[AnyRef, AnyRef]): Unit = value match {
    case stack: ItemStack =>
      val name = Item.REGISTRY.getNameForObject(stack.getItem).toString

      // Handle essentia/vis contents for Thaumcraft jars, phials, and crystals
      if ((name == "thaumcraft:jar_normal") ||
          (name == "thaumcraft:jar_void") ||
          (name == "thaumcraft:phial") ||
          (name == "thaumcraft:crystal_essence")) {
        if (stack.hasTagCompound &&
          stack.getTagCompound.hasKey("Aspects", NBT.TAG_LIST)) {
          val aspects = mutable.ArrayBuffer.empty[mutable.Map[String, Any]]
          val nbtAspects = stack.getTagCompound.getTagList("Aspects", NBT.TAG_COMPOUND).map {
            case tag: NBTTagCompound => tag
          }
          for (nbtAspect <- nbtAspects) {
            val key = nbtAspect.getString("key")
            val amount = nbtAspect.getInteger("amount")
            val aspect = mutable.Map[String, Any](
              "aspect" -> key,
              "amount" -> amount
            )
            aspects += aspect
          }
          output += "aspects" -> aspects
        }
        if (stack.hasTagCompound && stack.getTagCompound.hasKey("AspectFilter", NBT.TAG_STRING)) {
          output += "aspectFilter" -> stack.getTagCompound.getString("AspectFilter")
        }
      }

    case _ =>
  }
}
