package li.cil.oc.server.component

import java.util

import li.cil.oc.Constants
import li.cil.oc.Constants.DeviceInfo.DeviceAttribute
import li.cil.oc.Constants.DeviceInfo.DeviceClass
import li.cil.oc.Settings
import li.cil.oc.api.Network
import li.cil.oc.api.driver.DeviceInfo
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.api.internal
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network.Visibility
import li.cil.oc.api.prefab
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedArguments._
import li.cil.oc.util.ExtendedWorld._
import net.minecraft.init.Blocks
import net.minecraft.init.SoundEvents
import net.minecraft.util.EnumFacing
import net.minecraft.util.SoundCategory

import scala.collection.convert.WrapAsJava._

abstract class UpgradePiston(val host: EnvironmentHost) extends prefab.ManagedEnvironment with DeviceInfo {
  override val node = Network.newNode(this, Visibility.Network).
    withComponent("piston").
    withConnector().
    create()

  private final lazy val deviceInfo = Map(
    DeviceAttribute.Class -> DeviceClass.Generic,
    DeviceAttribute.Description -> "Piston upgrade",
    DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
    DeviceAttribute.Product -> "Displacer II+"
  )

  override def getDeviceInfo: util.Map[String, String] = deviceInfo

  def pushDirection(args: Arguments, index: Int): EnumFacing

  def pushOrigin(side: EnumFacing) = BlockPosition(host)

  @Callback(doc = """function([side:number]):boolean -- Tries to push the block on the specified side of the container of the upgrade. Defaults to front.""")
  def push(context: Context, args: Arguments): Array[AnyRef] = {
    val side = pushDirection(args, 0)
    val hostPos = pushOrigin(side)
    val blockPos = hostPos.offset(side)
    if (!host.world.isAirBlock(blockPos) && node.tryChangeBuffer(-Settings.get.pistonCost) && Blocks.PISTON.doMove(host.world, hostPos.toBlockPos, side, true)) {
      host.world.setBlockToAir(blockPos)
      host.world.playSound(null, host.xPosition, host.yPosition, host.zPosition, SoundEvents.BLOCK_PISTON_EXTEND, SoundCategory.BLOCKS, 0.5f, host.world.rand.nextFloat() * 0.25f + 0.6f)
      context.pause(0.5)
      result(true)
    }
    else result(false)
  }
}

object UpgradePiston {

  class Drone(drone: internal.Drone) extends UpgradePiston(drone) {
    override def pushDirection(args: Arguments, index: Int) = args.optSideAny(index, EnumFacing.SOUTH)
  }

  class Tablet(tablet: internal.Tablet) extends Rotatable(tablet) {
    override def pushOrigin(side: EnumFacing) =
      if (side == EnumFacing.DOWN && tablet.player.getEyeHeight > 1) super.pushOrigin(side).offset(EnumFacing.DOWN)
      else super.pushOrigin(side)
  }

  class Rotatable(val rotatable: internal.Rotatable with EnvironmentHost) extends UpgradePiston(rotatable) {
    override def pushDirection(args: Arguments, index: Int) = rotatable.toGlobal(args.optSideForAction(index, EnumFacing.SOUTH))
  }

}