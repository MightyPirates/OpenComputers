package li.cil.oc.common.tileentity

import java.util

import li.cil.oc.Constants
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute
import li.cil.oc.api.driver.DeviceInfo.DeviceClass
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.driver.DeviceInfo
import li.cil.oc.api.network._
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.Direction
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

import scala.collection.convert.ImplicitConversionsToJava._

class PowerConverter extends TileEntity(null) with traits.PowerAcceptor with traits.Environment with traits.NotAnalyzable with DeviceInfo {
  val node = api.Network.newNode(this, Visibility.None).
    withConnector(Settings.get.bufferConverter).
    create()

  private final lazy val deviceInfo = Map(
    DeviceAttribute.Class -> DeviceClass.Power,
    DeviceAttribute.Description -> "Power converter",
    DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
    DeviceAttribute.Product -> "Transgizer-PX5",
    DeviceAttribute.Capacity -> energyThroughput.toString
  )

  override def getDeviceInfo: util.Map[String, String] = deviceInfo

  @OnlyIn(Dist.CLIENT)
  override protected def hasConnector(side: Direction) = true

  override protected def connector(side: Direction) = Option(node)

  override def energyThroughput = Settings.get.powerConverterRate
}
