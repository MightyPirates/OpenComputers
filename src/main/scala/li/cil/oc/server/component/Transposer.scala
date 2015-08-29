package li.cil.oc.server.component

import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network.Visibility
import li.cil.oc.api.prefab
import li.cil.oc.common.tileentity
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedArguments._
import li.cil.oc.util.FluidUtils
import li.cil.oc.util.InventoryUtils
import net.minecraft.util.EnumFacing

import scala.language.existentials

class Transposer(val host: tileentity.Transposer) extends prefab.ManagedEnvironment with traits.WorldInventoryAnalytics with traits.WorldTankAnalytics {
  override val node = api.Network.newNode(this, Visibility.Network).
    withComponent("transposer").
    withConnector().
    create()

  override def position = BlockPosition(host)

  override protected def checkSideForAction(args: Arguments, n: Int) = args.checkSide(n, EnumFacing.values: _*)

  @Callback(doc = """function(sourceSide:number, sinkSide:number[, count:number[, sourceSlot:number, sinkSlot:number]]):number -- Transfer some items between two inventories.""")
  def transferItem(context: Context, args: Arguments): Array[AnyRef] = {
    val sourceSide = checkSideForAction(args, 0)
    val sourcePos = position.offset(sourceSide)
    val sinkSide = checkSideForAction(args, 1)
    val sinkPos = position.offset(sinkSide)
    val count = args.optItemCount(2)

    if (node.tryChangeBuffer(-Settings.get.transposerCost)) {
      ServerPacketSender.sendTransposerActivity(host)

      if (args.count > 3) {
        val sourceSlot = args.checkSlot(InventoryUtils.inventoryAt(sourcePos).getOrElse(throw new IllegalArgumentException("no inventory")), 3)
        val sinkSlot = args.checkSlot(InventoryUtils.inventoryAt(sinkPos).getOrElse(throw new IllegalArgumentException("no inventory")), 4)

        result(InventoryUtils.transferBetweenInventoriesSlotsAt(sourcePos, sourceSide.getOpposite, sourceSlot, sinkPos, Option(sinkSide.getOpposite), sinkSlot, count))
      }
      else result(InventoryUtils.transferBetweenInventoriesAt(sourcePos, sourceSide.getOpposite, sinkPos, Option(sinkSide.getOpposite), count))
    }
    else result(Unit, "not enough energy")
  }

  @Callback(doc = """function(sourceSide:number, sinkSide:number[, count:number]):number -- Transfer some items between two inventories.""")
  def transferFluid(context: Context, args: Arguments): Array[AnyRef] = {
    val sourceSide = checkSideForAction(args, 0)
    val sourcePos = position.offset(sourceSide)
    val sinkSide = checkSideForAction(args, 1)
    val sinkPos = position.offset(sinkSide)
    val count = args.optFluidCount(2)

    if (node.tryChangeBuffer(-Settings.get.transposerCost)) {
      ServerPacketSender.sendTransposerActivity(host)

      val moved = FluidUtils.transferBetweenFluidHandlersAt(sourcePos, sourceSide.getOpposite, sinkPos, sinkSide.getOpposite, count)
      if (moved > 0) context.pause(moved / 1000 * 0.25) // Allow up to 4 buckets per second.
      result(moved > 0, moved)
    }
    else result(Unit, "not enough energy")
  }
}
