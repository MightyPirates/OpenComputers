package li.cil.oc.integration.mfr

import java.util

import li.cil.oc.api.driver.Converter
import net.minecraft.item.ItemStack
import net.minecraftforge.common.util.Constants.NBT
import powercrystals.minefactoryreloaded.item.ItemSafariNet

import scala.collection.convert.WrapAsScala._

object ConverterSafariNet extends Converter {
  override def convert(value: scala.Any, output: util.Map[AnyRef, AnyRef]): Unit = value match {
    case stack: ItemStack if stack.getItem.isInstanceOf[ItemSafariNet] && stack.hasTagCompound =>
      val nbt = stack.getTagCompound
      if (nbt.hasKey("id", NBT.TAG_STRING) && !nbt.getBoolean("hide")) {
        output += "entity" -> nbt.getString("id")
      }
    case _ =>
  }
}
