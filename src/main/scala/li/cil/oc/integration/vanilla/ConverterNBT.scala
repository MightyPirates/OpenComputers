package li.cil.oc.integration.vanilla

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
    case tag: NBTTagByte => byte2Byte(tag.getByte)
    case tag: NBTTagShort => short2Short(tag.getShort)
    case tag: NBTTagInt => int2Integer(tag.getInt)
    case tag: NBTTagLong => long2Long(tag.getLong)
    case tag: NBTTagFloat => float2Float(tag.getFloat)
    case tag: NBTTagDouble => double2Double(tag.getDouble)
    case tag: NBTTagByteArray => tag.getByteArray
    case tag: NBTTagString => tag.getString
    case tag: NBTTagList =>
      val copy = tag.copy().asInstanceOf[NBTTagList]
      (0 until copy.tagCount).map(_ => convert(copy.removeTag(0))).toArray
    case tag: NBTTagCompound =>
      tag.getKeySet.collect {
        case key: String => key -> convert(tag.getTag(key))
      }.toMap
    case tag: NBTTagIntArray => tag.getIntArray
  }
}
