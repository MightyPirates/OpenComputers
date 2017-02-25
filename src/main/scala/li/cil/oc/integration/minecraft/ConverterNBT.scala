package li.cil.oc.integration.minecraft

import java.util

import li.cil.oc.api
import net.minecraft.nbt._

import scala.collection.convert.WrapAsScala._

object ConverterNBT extends api.driver.Converter {
  override def convert(value: AnyRef, output: util.Map[AnyRef, AnyRef]) =
    value match {
      case nbt: NBTTagCompound => output += "oc:flatten" -> convert(nbt)
      case _ =>
    }

  private def convert(nbt: NBTBase): AnyRef = nbt match {
    case tag: NBTTagByte => Byte.box(tag.getByte)
    case tag: NBTTagShort => Short.box(tag.getShort)
    case tag: NBTTagInt => Int.box(tag.getInt)
    case tag: NBTTagLong => Long.box(tag.getLong)
    case tag: NBTTagFloat => Float.box(tag.getFloat)
    case tag: NBTTagDouble => Double.box(tag.getDouble)
    case tag: NBTTagByteArray => tag.getByteArray
    case tag: NBTTagString => tag.getString
    case tag: NBTTagList =>
      val copy = tag.copy(): NBTTagList
      (0 until copy.tagCount).map(_ => convert(copy.removeTag(0))).toArray
    case tag: NBTTagCompound =>
      tag.getKeySet.collect {
        case key: String => key -> convert(tag.getTag(key))
      }.toMap
    case tag: NBTTagIntArray => tag.getIntArray
  }
}
