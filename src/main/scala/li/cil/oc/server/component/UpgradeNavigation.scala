package li.cil.oc.server.component

import li.cil.oc.api.{Rotatable, Network}
import li.cil.oc.api.network._
import li.cil.oc.common.component.ManagedComponent
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import li.cil.oc.util.ItemUtils.NavigationUpgradeData

class UpgradeNavigation(val owner: TileEntity) extends ManagedComponent {
  val node = Network.newNode(this, Visibility.Network).
    withComponent("navigation", Visibility.Neighbors).
    create()

  val data = new NavigationUpgradeData()

  // ----------------------------------------------------------------------- //

  @Callback(doc = """function():number, number, number -- Get the current relative position of the robot.""")
  def getPosition(context: Context, args: Arguments): Array[AnyRef] = {
    val info = data.mapData(owner.getWorldObj)
    val size = 128 * (1 << info.scale)
    val x = owner.xCoord
    val y = owner.yCoord
    val z = owner.zCoord
    val relativeX = x - info.xCenter
    val relativeZ = z - info.zCenter

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
    val info = data.mapData(owner.getWorldObj)
    val size = 128 * (1 << info.scale)
    result(size / 2)
  }

  // ----------------------------------------------------------------------- //

  override def load(nbt: NBTTagCompound) {
    super.load(nbt)
    data.load(nbt)
  }

  override def save(nbt: NBTTagCompound) {
    super.save(nbt)
    data.save(nbt)
  }
}
