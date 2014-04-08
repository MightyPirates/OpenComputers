package li.cil.oc.server.component

import li.cil.oc.api.network._
import li.cil.oc.api.{Rotatable, Network}
import li.cil.oc.Settings
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity

class UpgradeNavigation(val owner: TileEntity) extends ManagedComponent {
  val node = Network.newNode(this, Visibility.Network).
    withComponent("navigation", Visibility.Neighbors).
    create()

  var xCenter = owner.xCoord
  var zCenter = owner.zCoord
  var size = 512

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

  // ----------------------------------------------------------------------- //

  override def load(nbt: NBTTagCompound) {
    super.load(nbt)
    if (nbt.hasKey(Settings.namespace + "xCenter")) {
      xCenter = nbt.getInteger(Settings.namespace + "xCenter")
    }
    if (nbt.hasKey(Settings.namespace + "zCenter")) {
      zCenter = nbt.getInteger(Settings.namespace + "zCenter")
    }
    if (nbt.hasKey(Settings.namespace + "scale")) {
      size = nbt.getInteger(Settings.namespace + "scale")
    }
  }

  override def save(nbt: NBTTagCompound) {
    super.save(nbt)
    nbt.setInteger(Settings.namespace + "xCenter", xCenter)
    nbt.setInteger(Settings.namespace + "zCenter", zCenter)
    nbt.setInteger(Settings.namespace + "scale", size)
  }
}
