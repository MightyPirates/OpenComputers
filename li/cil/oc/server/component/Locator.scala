package li.cil.oc.server.component

import li.cil.oc.api.network._
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.{Settings, api}
import net.minecraft.entity.item.EntityItem
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.{TileEntity => MCTileEntity, TileEntitySign, TileEntityFurnace}
import scala.Some
import li.cil.oc.common.tileentity.Rotatable
import li.cil.oc.util.RotationHelper

class Locator(val owner: MCTileEntity, val xCenter: Int, val zCenter: Int,val scale:Int) extends ManagedComponent {
  val node = api.Network.newNode(this, Visibility.Network).
    withComponent("locator", Visibility.Neighbors).

    create()

  @LuaCallback("getPosition")
  def getPosition(context: RobotContext, args: Arguments): Array[AnyRef] = {
    val player = context.player()
    val x = player.posX.floor.toInt
    val z = player.posZ.floor.toInt
    val xDist = x - xCenter
    val zDist = z - zCenter

    if (math.abs(xDist) <= scale/2 && math.abs(zDist) <= scale/2)
      result(xDist, zDist)
    else
      result(Unit, "out of range")
  }
  @LuaCallback("getFacing")
  def getFacing(context: RobotContext, args: Arguments): Array[AnyRef] = {
    val player = context.player()
    val d = RotationHelper.fromYaw(player.rotationYaw)

    result(d.offsetX,d.offsetY,d.offsetZ)
  }

  // ----------------------------------------------------------------------- //

  override val canUpdate = false


  // ----------------------------------------------------------------------- //

}
