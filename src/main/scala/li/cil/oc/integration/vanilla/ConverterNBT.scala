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
    case tag: NBTTagByte => Byte.box(tag.func_150290_f())
    case tag: NBTTagShort => Short.box(tag.func_150289_e())
    case tag: NBTTagInt => Int.box(tag.func_150287_d())
    case tag: NBTTagLong => Long.box(tag.func_150291_c())
    case tag: NBTTagFloat => Float.box(tag.func_150288_h())
    case tag: NBTTagDouble => Double.box(tag.func_150286_g())
    case tag: NBTTagByteArray => tag.func_150292_c()
    case tag: NBTTagString => tag.func_150285_a_()
    case tag: NBTTagList =>
      val copy = tag.copy().asInstanceOf[NBTTagList]
      (0 until copy.tagCount).map(_ => convert(copy.removeTag(0))).toArray
    case tag: NBTTagCompound =>
      tag.func_150296_c().collect {
        case key: String => key -> convert(tag.getTag(key))
      }.toMap
    case tag: NBTTagIntArray => tag.func_150302_c()
  }
}
