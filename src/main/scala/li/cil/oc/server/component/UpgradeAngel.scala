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
import li.cil.oc.api.prefab.network.{AbstractManagedEnvironment, AbstractManagedEnvironment}

import scala.collection.convert.WrapAsJava._

// Note-to-self: this has a component to allow the robot telling it has the
// upgrade.
class UpgradeAngel extends AbstractManagedEnvironment with DeviceInfo {
  private final lazy val deviceInfo = Map(
    DeviceAttribute.Class -> DeviceClass.Generic,
    DeviceAttribute.Description -> "Angel upgrade",
    DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
    DeviceAttribute.Product -> "FreePlacer (TM)",
    DeviceAttribute.Capacity -> Settings.get.maxNetworkPacketSize.toString
  )

  override def getDeviceInfo: util.Map[String, String] = deviceInfo
}
