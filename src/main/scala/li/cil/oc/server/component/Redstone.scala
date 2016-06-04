package li.cil.oc.server.component

import java.util

import li.cil.oc.Constants
import li.cil.oc.Constants.DeviceInfo.DeviceAttribute
import li.cil.oc.Constants.DeviceInfo.DeviceClass
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.common.tileentity.traits.BundledRedstoneAware
import li.cil.oc.common.tileentity.traits.RedstoneAware
import li.cil.oc.server.component

import scala.collection.convert.WrapAsJava._

object Redstone {

  class Vanilla(val redstone: EnvironmentHost with RedstoneAware)
    extends component.RedstoneVanilla

  class Bundled(val redstone: EnvironmentHost with BundledRedstoneAware)
    extends component.RedstoneVanilla with component.RedstoneBundled

  class Wireless(val redstone: EnvironmentHost)
    extends component.RedstoneWireless

  class VanillaWireless(val redstone: EnvironmentHost with RedstoneAware)
    extends component.RedstoneVanilla with component.RedstoneWireless

  class BundledWireless(val redstone: EnvironmentHost with BundledRedstoneAware)
    extends component.RedstoneVanilla with component.RedstoneBundled with component.RedstoneWireless {
    private final lazy val deviceInfo = Map(
      DeviceAttribute.Class -> DeviceClass.Communication,
      DeviceAttribute.Description -> "Combined redstone controller",
      DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
      DeviceAttribute.Product -> "Rx900-M",
      DeviceAttribute.Capacity -> "65536",
      DeviceAttribute.Width -> "16"
    )

    override def getDeviceInfo: util.Map[String, String] = deviceInfo
  }

}
