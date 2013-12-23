package li.cil.oc.server.component

import li.cil.oc.api
import li.cil.oc.api.network._
import li.cil.oc.util.RotationHelper
import net.minecraft.tileentity.{TileEntity => MCTileEntity}

class Locator(val owner: MCTileEntity, val xCenter: Int, val zCenter: Int, val scale: Int) extends ManagedComponent {
  val node = api.Network.newNode(this, Visibility.Network).
    withComponent("locator", Visibility.Neighbors).
    create()

  // ----------------------------------------------------------------------- //

  override val canUpdate = false

  // ----------------------------------------------------------------------- //

  @LuaCallback("getPosition")
  def getPosition(context: RobotContext, args: Arguments): Array[AnyRef] = {
    val x = owner.xCoord
    val z = owner.zCoord
    val y = owner.yCoord
    val relativeX = x - xCenter
    val relativeY = z - zCenter

    if (math.abs(relativeX) <= scale / 2 && math.abs(relativeY) <= scale / 2)
      result(relativeX, relativeY, y)
    else
      result(Unit, "out of range")
  }

  @LuaCallback("getFacing")
  def getFacing(context: RobotContext, args: Arguments): Array[AnyRef] = {
    result(RotationHelper.fromYaw(context.player().rotationYaw).ordinal())
  }
}
