package li.cil.oc.server.component

import java.util

import li.cil.oc.Constants
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute
import li.cil.oc.api.driver.DeviceInfo.DeviceClass
import li.cil.oc.Settings
import li.cil.oc.api.Network
import li.cil.oc.api.driver.DeviceInfo
import li.cil.oc.api.network.Visibility
import li.cil.oc.api.prefab

import scala.collection.convert.WrapAsJava._

class Memory(val tier: Int) extends prefab.ManagedEnvironment with DeviceInfo {
  override val node = Network.newNode(this, Visibility.Network).
    create()

  private final lazy val deviceInfo = Map(
    DeviceAttribute.Class -> DeviceClass.Memory,
    DeviceAttribute.Description -> "Memory bank",
    DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
    DeviceAttribute.Product -> "Multipurpose RAM Type",
    DeviceAttribute.Clock -> (Settings.get.callBudgets(tier) * 1000).toInt.toString
  )

  override def getDeviceInfo: util.Map[String, String] = deviceInfo
}
