package li.cil.oc.server.component

import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.driver.EnvironmentHost
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.network.Visibility
import li.cil.oc.api.prefab
import li.cil.oc.common.tileentity
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedArguments._
import net.minecraftforge.common.util.ForgeDirection

import scala.language.existentials

object Transposer {

  abstract class Common extends prefab.ManagedEnvironment with traits.WorldInventoryAnalytics with traits.WorldTankAnalytics with traits.InventoryTransfer {
    override val node = api.Network.newNode(this, Visibility.Network).
      withComponent("transposer").
      withConnector().
      create()

    override protected def checkSideForAction(args: Arguments, n: Int) =
      args.checkSide(n, ForgeDirection.VALID_DIRECTIONS: _*)

    override def onTransferContents(): Option[String] = {
      if (node.tryChangeBuffer(-Settings.get.transposerCost)) None
      else Option("not enough energy")
    }
  }

  class Block(val host: tileentity.Transposer) extends Common {
    override def position = BlockPosition(host)

    override def onTransferContents(): Option[String] = {
      val result = super.onTransferContents()
      if (result.isEmpty) ServerPacketSender.sendTransposerActivity(host)
      result
    }
  }

  class Upgrade(val host: EnvironmentHost) extends Common {
    node.setVisibility(Visibility.Neighbors)

    override def position = BlockPosition(host)
  }

}
