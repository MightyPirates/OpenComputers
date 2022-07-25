package li.cil.oc.common.tileentity

import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network.Visibility
import li.cil.oc.common.EventHandler
import li.cil.oc.server.network.Waypoints
import net.minecraft.nbt.CompoundNBT
import net.minecraft.particles.ParticleTypes
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.Direction
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

class Waypoint extends TileEntity(null) with traits.Environment with traits.Rotatable with traits.RedstoneAware with traits.Tickable {
  val node = api.Network.newNode(this, Visibility.Network).
    withComponent("waypoint").
    create()

  var label = ""

  override def validFacings: Array[Direction]  = Direction.values

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
      val origin = position.toVec3.add(facing.getStepX * 0.5, facing.getStepY * 0.5, facing.getStepZ * 0.5)
      val dx = (getLevel.random.nextFloat() - 0.5f) * 0.8f
      val dy = (getLevel.random.nextFloat() - 0.5f) * 0.8f
      val dz = (getLevel.random.nextFloat() - 0.5f) * 0.8f
      val vx = (getLevel.random.nextFloat() - 0.5f) * 0.2f + facing.getStepX * 0.3f
      val vy = (getLevel.random.nextFloat() - 0.5f) * 0.2f + facing.getStepY * 0.3f - 0.5f
      val vz = (getLevel.random.nextFloat() - 0.5f) * 0.2f + facing.getStepZ * 0.3f
      getLevel.addParticle(ParticleTypes.PORTAL, origin.x + dx, origin.y + dy, origin.z + dz, vx, vy, vz)
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

  private final val LabelTag = Settings.namespace + "label"

  override def loadForServer(nbt: CompoundNBT): Unit = {
    super.loadForServer(nbt)
    label = nbt.getString(LabelTag)
  }

  override def saveForServer(nbt: CompoundNBT): Unit = {
    super.saveForServer(nbt)
    nbt.putString(LabelTag, label)
  }

  @OnlyIn(Dist.CLIENT) override
  def loadForClient(nbt: CompoundNBT): Unit = {
    super.loadForClient(nbt)
    label = nbt.getString(LabelTag)
  }

  override def saveForClient(nbt: CompoundNBT): Unit = {
    super.saveForClient(nbt)
    nbt.putString(LabelTag, label)
  }
}
