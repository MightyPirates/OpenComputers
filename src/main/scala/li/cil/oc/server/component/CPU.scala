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
import li.cil.oc.api.prefab.network.{AbstractManagedNodeContainer, AbstractManagedNodeContainer}

import scala.collection.convert.WrapAsJava._

class CPU(val tier: Int) extends AbstractManagedNodeContainer with DeviceInfo {
  override val getNode = Network.newNode(this, Visibility.NEIGHBORS).
    create()

  private final lazy val deviceInfo = Map(
    DeviceAttribute.Class -> DeviceClass.Processor,
    DeviceAttribute.Description -> "CPU",
    DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
    DeviceAttribute.Product -> ("FlexiArch " + (tier + 1).toString + " Processor"),
    DeviceAttribute.Clock -> (Settings.get.callBudgets(tier) * 1000).toInt.toString
  )

  override def getDeviceInfo: util.Map[String, String] = deviceInfo
}
