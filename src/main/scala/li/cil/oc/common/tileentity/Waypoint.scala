package li.cil.oc.common.tileentity

import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network.Visibility
import li.cil.oc.common.EventHandler
import li.cil.oc.server.network.Waypoints
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.ForgeDirection

class Waypoint extends traits.Environment with traits.Rotatable with traits.RedstoneAware {
  val node = api.Network.newNode(this, Visibility.Network).
    withComponent("waypoint").
    create()

  var label = ""

  override def validFacings: Array[ForgeDirection] = ForgeDirection.VALID_DIRECTIONS

  // ----------------------------------------------------------------------- //

  @Callback(doc = """function(): string -- Get the current label of this waypoint.""")
  def getLabel(context: Context, args: Arguments): Array[Object] = result(label)

  @Callback(doc = """function(value:string) -- Set the label for this waypoint.""")
  def setLabel(context: Context, args: Arguments): Array[Object] = {
    label = args.checkString(0).take(32)
    context.pause(0.5)
    null
  }

  // ----------------------------------------------------------------------- //

  override def updateEntity(): Unit = {
    super.updateEntity()
    if (isClient) {
      val origin = position.toVec3.addVector(facing.offsetX * 0.5, facing.offsetY * 0.5, facing.offsetZ * 0.5)
      val dx = (world.rand.nextFloat() - 0.5f) * 0.8f
      val dy = (world.rand.nextFloat() - 0.5f) * 0.8f
      val dz = (world.rand.nextFloat() - 0.5f) * 0.8f
      val vx = (world.rand.nextFloat() - 0.5f) * 0.2f + facing.offsetX * 0.3f
      val vy = (world.rand.nextFloat() - 0.5f) * 0.2f + facing.offsetY * 0.3f - 0.5f
      val vz = (world.rand.nextFloat() - 0.5f) * 0.2f + facing.offsetZ * 0.3f
      world.spawnParticle("portal", origin.xCoord + dx, origin.yCoord + dy, origin.zCoord + dz, vx, vy, vz)
    }
  }

  override protected def initialize(): Unit = {
    super.initialize()
    EventHandler.scheduleServer(() => Waypoints.add(this))
  }

  override def dispose(): Unit = {
    super.dispose()
    Waypoints.remove(this)
  }

  // ----------------------------------------------------------------------- //

  override def readFromNBTForServer(nbt: NBTTagCompound): Unit = {
    super.readFromNBTForServer(nbt)
    label = nbt.getString(Settings.namespace + "label")
  }

  override def writeToNBTForServer(nbt: NBTTagCompound): Unit = {
    super.writeToNBTForServer(nbt)
    nbt.setString(Settings.namespace + "label", label)
  }

  @SideOnly(Side.CLIENT) override
  def readFromNBTForClient(nbt: NBTTagCompound): Unit = {
    super.readFromNBTForClient(nbt)
    label = nbt.getString(Settings.namespace + "label")
  }

  override def writeToNBTForClient(nbt: NBTTagCompound): Unit = {
    super.writeToNBTForClient(nbt)
    nbt.setString(Settings.namespace + "label", label)
  }
}
