package li.cil.oc.server.component

import java.util

import li.cil.oc.Constants
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute
import li.cil.oc.api.driver.DeviceInfo.DeviceClass
import li.cil.oc.Settings

import scala.collection.convert.WrapAsJava._

class APU(tier: Int) extends GraphicsCard(tier) {
  private final lazy val deviceInfo = Map(
    DeviceAttribute.Class -> DeviceClass.Processor,
    DeviceAttribute.Description -> "APU",
    DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
    DeviceAttribute.Product -> ("FlexiArch " + (tier + 1).toString + " Processor (Builtin Graphics)"),
    DeviceAttribute.Capacity -> capacityInfo,
    DeviceAttribute.Width -> widthInfo,
    DeviceAttribute.Clock -> ((Settings.get.callBudgets(tier) * 1000).toInt.toString + "+" + clockInfo)
  )

  override def getDeviceInfo: util.Map[String, String] = deviceInfo
}
