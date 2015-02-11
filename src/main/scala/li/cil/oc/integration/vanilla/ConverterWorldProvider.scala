package li.cil.oc.integration.vanilla

import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util
import java.util.UUID

import li.cil.oc.api
import net.minecraft.world

import scala.collection.convert.WrapAsScala._

object ConverterWorldProvider extends api.driver.Converter {
  override def convert(value: AnyRef, output: util.Map[AnyRef, AnyRef]) =
    value match {
      case provider: world.WorldProvider =>
        val digest = MessageDigest.getInstance("MD5")

        digest.update(ByteBuffer.allocate(8).putLong(provider.getSeed).array)
        digest.update(ByteBuffer.allocate(4).putInt(provider.dimensionId).array)

        output += "id" -> UUID.nameUUIDFromBytes(digest.digest()).toString
        output += "name" -> provider.getDimensionName
      case _ =>
    }
}
