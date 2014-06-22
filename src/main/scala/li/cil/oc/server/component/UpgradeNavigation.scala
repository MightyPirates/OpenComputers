package li.cil.oc.server.component

import li.cil.oc.api.driver.Container
import li.cil.oc.api.network._
import li.cil.oc.api.{Network, Rotatable}
import li.cil.oc.common.component
import li.cil.oc.util.ItemUtils.NavigationUpgradeData
import net.minecraft.nbt.NBTTagCompound

class UpgradeNavigation(val owner: Container with Rotatable) extends component.ManagedComponent {
  val node = Network.newNode(this, Visibility.Network).
    withComponent("navigation", Visibility.Neighbors).
    create()

  val data = new NavigationUpgradeData()

  // ----------------------------------------------------------------------- //

  @Callback(doc = """function():number, number, number -- Get the current relative position of the robot.""")
  def getPosition(context: Context, args: Arguments): Array[AnyRef] = {
    val info = data.mapData(owner.world)
    val size = 128 * (1 << info.scale)
    val relativeX = owner.xPosition - info.xCenter
    val relativeZ = owner.zPosition - info.zCenter

    if (math.abs(relativeX) <= size / 2 && math.abs(relativeZ) <= size / 2)
      result(relativeX, owner.yPosition, relativeZ)
    else
      result(Unit, "out of range")
  }

  @Callback(doc = """function():number -- Get the current orientation of the robot.""")
  def getFacing(context: Context, args: Arguments): Array[AnyRef] = result(owner.facing.ordinal)

  @Callback(doc = """function():number -- Get the operational range of the navigation upgrade.""")
  def getRange(context: Context, args: Arguments): Array[AnyRef] = {
    val info = data.mapData(owner.world)
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
