package li.cil.oc.integration.minecraft

import java.util

import li.cil.oc.api
import net.minecraft.nbt._

import scala.collection.convert.ImplicitConversionsToScala._

object ConverterNBT extends api.driver.Converter {
  override def convert(value: AnyRef, output: util.Map[AnyRef, AnyRef]) =
    value match {
      case nbt: CompoundNBT => output += "oc:flatten" -> convert(nbt)
      case _ =>
    }

  private def convert(nbt: INBT): AnyRef = nbt match {
    case tag: ByteNBT => Byte.box(tag.getAsByte)
    case tag: ShortNBT => Short.box(tag.getAsShort)
    case tag: IntNBT => Int.box(tag.getAsInt)
    case tag: LongNBT => Long.box(tag.getAsLong)
    case tag: FloatNBT => Float.box(tag.getAsFloat)
    case tag: DoubleNBT => Double.box(tag.getAsDouble)
    case tag: ByteArrayNBT => tag.getAsByteArray
    case tag: StringNBT => tag.getAsString
    case tag: ListNBT =>
      val copy = tag.copy(): ListNBT
      (0 until copy.size).map(_ => convert(copy.remove(0))).toArray
    case tag: CompoundNBT =>
      tag.getAllKeys.collect {
        case key: String => key -> convert(tag.get(key))
      }.toMap
    case tag: IntArrayNBT => tag.getAsIntArray
  }
}
