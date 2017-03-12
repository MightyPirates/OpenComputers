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
import li.cil.oc.api.prefab.network.{AbstractManagedNodeContainer, AbstractManagedNodeHost}

import scala.collection.convert.WrapAsJava._

class UpgradeBattery(val tier: Int) extends AbstractManagedNodeContainer with DeviceInfo {
  override val getNode = Network.newNode(this, Visibility.NETWORK).
    withConnector(Settings.get.bufferCapacitorUpgrades(tier)).
    create()

  private final lazy val deviceInfo = Map(
    DeviceAttribute.Class -> DeviceClass.Power,
    DeviceAttribute.Description -> "Battery",
    DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
    DeviceAttribute.Product -> "Unlimited Power (Almost Ed.)",
    DeviceAttribute.Capacity -> Settings.get.bufferCapacitorUpgrades(tier).toString
  )

  override def getDeviceInfo: util.Map[String, String] = deviceInfo
}
