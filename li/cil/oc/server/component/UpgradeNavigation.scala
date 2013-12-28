package li.cil.oc.server.component

import li.cil.oc.api
import li.cil.oc.api.network._
import li.cil.oc.util.RotationHelper
import net.minecraft.tileentity.{TileEntity => MCTileEntity}

class UpgradeNavigation(val owner: MCTileEntity, val xCenter: Int, val zCenter: Int, val size: Int) extends ManagedComponent {
  val node = api.Network.newNode(this, Visibility.Network).
    withComponent("navigation", Visibility.Neighbors).
    create()

  // ----------------------------------------------------------------------- //

  @LuaCallback("getPosition")
  def getPosition(context: RobotContext, args: Arguments): Array[AnyRef] = {
    val x = owner.xCoord
    val z = owner.zCoord
    val y = owner.yCoord
    val relativeX = x - xCenter
    val relativeY = z - zCenter

    if (math.abs(relativeX) <= size / 2 && math.abs(relativeY) <= size / 2)
      result(relativeX, relativeY, y)
    else
      result(Unit, "out of range")
  }

  @LuaCallback("getFacing")
  def getFacing(context: RobotContext, args: Arguments): Array[AnyRef] = {
    result(RotationHelper.fromYaw(context.player().rotationYaw).ordinal())
  }

  @LuaCallback("getRange")
  def getRange(context: RobotContext, args: Arguments): Array[AnyRef] = {
    result(size / 2)
  }
}
