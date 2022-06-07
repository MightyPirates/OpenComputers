package li.cil.oc.server.component

import java.util

import li.cil.oc.Constants
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute
import li.cil.oc.api.driver.DeviceInfo.DeviceClass
import li.cil.oc.api.Network
import li.cil.oc.api.driver.DeviceInfo
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.api.internal
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network.Visibility
import li.cil.oc.api.prefab.AbstractManagedEnvironment
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedArguments._
import net.minecraft.block.BlockPistonBase
import net.minecraft.init.SoundEvents
import net.minecraft.util.{EnumFacing, SoundCategory}
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import net.minecraft.block.material.EnumPushReaction

import scala.collection.convert.WrapAsJava._

protected object PistonTraits {
  trait ExtendAware {
    val host: EnvironmentHost
    def pushOrigin(side: EnumFacing): BlockPosition = BlockPosition(host)
    def pushDirection(args: Arguments, index: Int): EnumFacing
  }

  trait DroneLike extends ExtendAware {
    def pushDirection(args: Arguments, index: Int): EnumFacing = args.optSideAny(index, EnumFacing.SOUTH)
  }

  trait RotatableLike extends ExtendAware {
    val rotatable: internal.Rotatable with EnvironmentHost
    def pushDirection(args: Arguments, index: Int): EnumFacing = rotatable.toGlobal(args.optSideForAction(index, EnumFacing.SOUTH))
  }

  trait TabletLike extends ExtendAware {
    val tablet: internal.Tablet
    override def pushOrigin(side: EnumFacing): BlockPosition =
      if (side == EnumFacing.DOWN && tablet.player.getEyeHeight > 1) super.pushOrigin(side).offset(EnumFacing.DOWN)
      else super.pushOrigin(side)
  }
}

abstract class UpgradePiston(val host: EnvironmentHost) extends AbstractManagedEnvironment with DeviceInfo with PistonTraits.ExtendAware {
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

  val isSticky: Boolean = false

  @Callback(doc = """function():boolean -- Returns true if the piston is sticky, i.e. it can also pull.""")
  def isSticky(context: Context, args: Arguments): Array[AnyRef] = result(isSticky)

  protected def doPistonAction(context: Context, side: EnumFacing, extending: Boolean): Array[AnyRef] = {
    val sound = if (extending) SoundEvents.BLOCK_PISTON_EXTEND.getRegistryName else SoundEvents.BLOCK_PISTON_CONTRACT.getRegistryName
    val hostPos = pushOrigin(side).toBlockPos
    val piston = new BlockPistonBase(isSticky)

    if (!extending) {
      if (!isSticky) {
        // this is a bug in oc code
        throw new NoSuchMethodError("piston is not sticky. does not have pull")
      }
      // make sure that any obstruction block has breaking mobility
      val innerBlockPos = hostPos.offset(side)
      val innerBlockState = host.world.getBlockState(innerBlockPos)
      if (innerBlockState != null) {
        if (!innerBlockState.getBlock.isAir(innerBlockState, host.world, innerBlockPos)) {
          if (innerBlockState.getPushReaction != EnumPushReaction.DESTROY) {
            return result(false, "path is obstructed")
          }
        }
      }
    }

    if (piston.doMove(host.world, hostPos, side, extending)) {
      // send piston extend sound to clients
      host.synchronized(ServerPacketSender.sendSound(
        host.world, hostPos.getX, hostPos.getY, hostPos.getZ,
        sound, SoundCategory.BLOCKS, range = 15.0))
      context.pause(1.0 / 20.0)
      result(true)
    } else {
      result(false, "move failed")
    }
  }

  @Callback(doc = """function([side:number]):boolean -- Tries to push the block on the specified side of the container of the upgrade. Defaults to front.""")
  def push(context: Context, args: Arguments): Array[AnyRef] = {
    val side = pushDirection(args, index = 0)
    doPistonAction(context, side, true)
  }
}

abstract class UpgradeStickyPiston(host: EnvironmentHost) extends UpgradePiston(host) {
  override val isSticky: Boolean = true

  @Callback(doc = """function([side:number]):boolean -- Tries to reach out to the side given (default front) and pull a block similar to a vanilla sticky piston.""")
  def pull(context: Context, args: Arguments): Array[AnyRef] = {
    val side = pushDirection(args, index = 0)
    doPistonAction(context, side, false)
  }
}

object UpgradePiston {
  class Drone(drone: internal.Drone) extends UpgradePiston(drone) with PistonTraits.DroneLike

  class Rotatable(val rotatable: internal.Rotatable with EnvironmentHost) extends UpgradePiston(rotatable) with PistonTraits.RotatableLike

  class Tablet(val tablet: internal.Tablet) extends Rotatable(tablet) with PistonTraits.TabletLike
}

object UpgradeStickyPiston {
  class Drone(drone: internal.Drone) extends UpgradeStickyPiston(drone) with PistonTraits.DroneLike

  class Rotatable(val rotatable: internal.Rotatable with EnvironmentHost) extends UpgradeStickyPiston(rotatable) with PistonTraits.RotatableLike

  class Tablet(val tablet: internal.Tablet) extends Rotatable(tablet) with PistonTraits.TabletLike
}
