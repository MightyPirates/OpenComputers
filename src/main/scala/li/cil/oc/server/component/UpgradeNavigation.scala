package li.cil.oc.server.component

import li.cil.oc.api.network._
import li.cil.oc.api.{Rotatable, Network}
import net.minecraft.tileentity.TileEntity

class UpgradeNavigation(val owner: TileEntity, val xCenter: Int, val zCenter: Int, val size: Int) extends ManagedComponent {
  val node = Network.newNode(this, Visibility.Network).
    withComponent("navigation", Visibility.Neighbors).
    create()

  // ----------------------------------------------------------------------- //

  @Callback(doc = """function():number, number, number -- Get the current relative position of the robot.""")
  def getPosition(context: Context, args: Arguments): Array[AnyRef] = {
    val x = owner.xCoord
    val y = owner.yCoord
    val z = owner.zCoord
    val relativeX = x - xCenter
    val relativeZ = z - zCenter

    if (math.abs(relativeX) <= size / 2 && math.abs(relativeZ) <= size / 2)
      result(relativeX, y, relativeZ)
    else
      result(Unit, "out of range")
  }

  @Callback(doc = """function():number -- Get the current orientation of the robot.""")
  def getFacing(context: Context, args: Arguments): Array[AnyRef] = {
    owner match {
      case rotatable: Rotatable => result(rotatable.facing.ordinal)
      case _ => throw new Exception("illegal state")
    }
  }

  @Callback(doc = """function():number -- Get the operational range of the navigation upgrade.""")
  def getRange(context: Context, args: Arguments): Array[AnyRef] = {
    result(size / 2)
  }
}
