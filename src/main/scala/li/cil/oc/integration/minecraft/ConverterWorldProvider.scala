package li.cil.oc.integration.minecraft

import java.util
import java.util.UUID

import com.google.common.hash.Hashing
import li.cil.oc.api
import net.minecraft.world

import scala.collection.convert.WrapAsScala._

object ConverterWorldProvider extends api.driver.Converter {
  override def convert(value: AnyRef, output: util.Map[AnyRef, AnyRef]) =
    value match {
      case provider: world.WorldProvider =>
        output += "id" -> UUID.nameUUIDFromBytes(Hashing.md5().newHasher().
          putLong(provider.getSeed).
          putInt(provider.getDimension).
          hash().asBytes()).toString
        output += "name" -> provider.getDimensionType.getName
      case _ =>
    }
}
