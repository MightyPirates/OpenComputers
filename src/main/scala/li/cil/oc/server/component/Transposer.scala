package li.cil.oc.server.component

import java.util

import li.cil.oc.Constants
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute
import li.cil.oc.api.driver.DeviceInfo.DeviceClass
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.driver.DeviceInfo
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.network.{Environment, Visibility}
import li.cil.oc.api.prefab
import li.cil.oc.api.prefab.network.{AbstractManagedNodeContainer, AbstractManagedNodeHost}
import li.cil.oc.api.util.Location
import li.cil.oc.common.tileentity
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedArguments._

import scala.collection.convert.WrapAsJava._
import scala.language.existentials

object Transposer {

  abstract class Common extends AbstractManagedNodeContainer with traits.WorldInventoryAnalytics with traits.WorldTankAnalytics with traits.InventoryTransfer with DeviceInfo {
    override val getNode = api.Network.newNode(this, Visibility.NETWORK).
      withComponent("transposer").
      withConnector().
      create()

    private final lazy val deviceInfo = Map(
      DeviceAttribute.Class -> DeviceClass.Generic,
      DeviceAttribute.Description -> "Transposer",
      DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
      DeviceAttribute.Product -> "TP4k-iX"
    )

    override def getDeviceInfo: util.Map[String, String] = deviceInfo

    override protected def checkSideForAction(args: Arguments, n: Int) =
      args.checkSideAny(n)

    override def onTransferContents(): Option[String] = {
      if (getNode.tryChangeEnergy(-Settings.get.transposerCost)) None
      else Option("not enough energy")
    }
  }

  class Block(val host: tileentity.TileEntityTransposer) extends Common {
    override def position = BlockPosition(host)

    override def onTransferContents(): Option[String] = {
      val result = super.onTransferContents()
      if (result.isEmpty) ServerPacketSender.sendTransposerActivity(host)
      result
    }
  }

  class Upgrade(val host: Location) extends Common {
    getNode.setVisibility(Visibility.NEIGHBORS)

    override def position = BlockPosition(host)
  }

}
