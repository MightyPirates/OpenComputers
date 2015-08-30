package li.cil.oc.integration.betterrecords

import java.util

import li.cil.oc.api.driver.Converter
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.Constants.NBT

import scala.collection.convert.WrapAsScala._

object ConverterRecord extends Converter {
  final val UrlRecordClassName = "com.codingforcookies.betterrecords.src.items.ItemURLRecord"
  final val UrlMultiRecordClassName = "com.codingforcookies.betterrecords.src.items.ItemURLMultiRecord"
  final val FreqCrystalClassName = "com.codingforcookies.betterrecords.src.items.ItemFreqCrystal"

  override def convert(value: scala.Any, output: util.Map[AnyRef, AnyRef]): Unit = value match {
    case stack: ItemStack if stack.getItem != null && stack.getItem.getClass.getName == UrlRecordClassName && stack.hasTagCompound =>
      convertRecord(stack.getTagCompound, output)
    case stack: ItemStack if stack.getItem != null && stack.getItem.getClass.getName == UrlMultiRecordClassName && stack.hasTagCompound =>
      output += "songs" -> stack.getTagCompound.getTagList("songs", NBT.TAG_COMPOUND).map((nbt: NBTTagCompound) => convertRecord(nbt, new util.HashMap[AnyRef, AnyRef]()))
    case stack: ItemStack if stack.getItem != null && stack.getItem.getClass.getName == FreqCrystalClassName && stack.hasTagCompound =>
      convertRecord(stack.getTagCompound, output)
    case _ =>
  }

  private def convertRecord(nbt: NBTTagCompound, output: util.Map[AnyRef, AnyRef]) = {
    if (nbt.hasKey("url", NBT.TAG_STRING))
      output += "url" -> nbt.getString("url")
    if (nbt.hasKey("name", NBT.TAG_STRING))
      output += "filename" -> nbt.getString("name")
    if (nbt.hasKey("author", NBT.TAG_STRING))
      output += "author" -> nbt.getString("author")
    if (nbt.hasKey("local", NBT.TAG_STRING))
      output += "title" -> nbt.getString("local")

    output
  }
}
