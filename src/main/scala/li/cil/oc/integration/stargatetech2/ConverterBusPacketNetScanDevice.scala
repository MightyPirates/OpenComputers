package li.cil.oc.integration.stargatetech2

import java.util

import li.cil.oc.api
import lordfokas.stargatetech2.api.bus.BusPacketNetScan

import scala.collection.convert.WrapAsScala._

object ConverterBusPacketNetScanDevice extends api.driver.Converter {
  override def convert(value: scala.Any, output: util.Map[AnyRef, AnyRef]) =
    value match {
      case device: BusPacketNetScan.Device =>
        output += "address" -> Short.box(device.address)
        output += "name" -> device.name
        output += "description" -> device.description
        output += "enabled" -> Boolean.box(device.enabled)
        output += "x" -> Int.box(device.x)
        output += "y" -> Int.box(device.y)
        output += "z" -> Int.box(device.z)
      case _ =>
    }
}
