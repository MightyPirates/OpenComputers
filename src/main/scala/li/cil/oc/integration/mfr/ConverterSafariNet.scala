package li.cil.oc.integration.mfr

import java.util

import li.cil.oc.api.driver.Converter
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraftforge.common.util.Constants.NBT

import scala.collection.convert.WrapAsScala._

object ConverterSafariNet extends Converter {
  private val SafariNetNames = Set(
    "MineFactoryReloaded:item.mfr.safarinet.reusable",
    "MineFactoryReloaded:item.mfr.safarinet.singleuse",
    "MineFactoryReloaded:item.mfr.safarinet.jailer")

  override def convert(value: scala.Any, output: util.Map[AnyRef, AnyRef]): Unit = value match {
    case stack: ItemStack =>
      val name = Item.itemRegistry.getNameForObject(stack.getItem)
      if (SafariNetNames.contains(name)) {
        val nbt = stack.getTagCompound
        if (nbt.hasKey("id", NBT.TAG_STRING) && !nbt.getBoolean("hide")) {
          output += "entity" -> nbt.getString("id")
        }
      }
    case _ =>
  }
}
