package li.cil.oc.common.tileentity

import java.util

import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import li.cil.oc.Constants
import li.cil.oc.Constants.DeviceInfo.DeviceAttribute
import li.cil.oc.Constants.DeviceInfo.DeviceClass
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.driver.DeviceInfo
import li.cil.oc.api.network._
import net.minecraftforge.common.util.ForgeDirection

import scala.collection.convert.WrapAsJava._

class PowerConverter extends traits.PowerAcceptor with traits.Environment with traits.NotAnalyzable with DeviceInfo {
  val node = api.Network.newNode(this, Visibility.None).
    withConnector(Settings.get.bufferConverter).
    create()

  private final val deviceInfo = Map(
    DeviceAttribute.Class -> DeviceClass.Power,
    DeviceAttribute.Description -> "Power converter",
    DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
    DeviceAttribute.Product -> "Transgizer-PX5",
    DeviceAttribute.Capacity -> energyThroughput.toString
  )

  override def getDeviceInfo: util.Map[String, String] = deviceInfo

  @SideOnly(Side.CLIENT)
  override protected def hasConnector(side: ForgeDirection) = true

  override protected def connector(side: ForgeDirection) = Option(node)

  override def energyThroughput = Settings.get.powerConverterRate

  override def canUpdate = isServer
}
