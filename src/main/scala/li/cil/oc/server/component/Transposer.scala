package li.cil.oc.server.component

import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.network.Visibility
import li.cil.oc.api.prefab
import li.cil.oc.common.tileentity
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedArguments._
import net.minecraftforge.common.util.ForgeDirection

import scala.language.existentials

class Transposer(val host: tileentity.Transposer) extends prefab.ManagedEnvironment with traits.WorldInventoryAnalytics with traits.WorldTankAnalytics with traits.InventoryTransfer {
  override val node = api.Network.newNode(this, Visibility.Network).
    withComponent("transposer").
    withConnector().
    create()

  override def position = BlockPosition(host)

  override protected def checkSideForAction(args: Arguments, n: Int) =
    args.checkSide(n, ForgeDirection.VALID_DIRECTIONS: _*)

  override def onTransferContents(): Option[String] = {
    if (node.tryChangeBuffer(-Settings.get.transposerCost)) {
      ServerPacketSender.sendTransposerActivity(host)
      None
    }
    else Option("not enough energy")
  }
}
