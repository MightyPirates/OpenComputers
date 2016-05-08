package li.cil.oc.common.tileentity

import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network.Visibility
import net.minecraft.entity.EntityLivingBase
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.potion.Potion
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.Vec3d

import scala.collection.convert.WrapAsScala._
import scala.collection.mutable

class MotionSensor extends traits.Environment {
  val node = api.Network.newNode(this, Visibility.Network).
    withComponent("motion_sensor").
    withConnector().
    create()

  private val radius = 8

  private var sensitivity = 0.4

  private val trackedEntities = mutable.Map.empty[EntityLivingBase, (Double, Double, Double)]

  // ----------------------------------------------------------------------- //

  override def canUpdate = isServer

  override def updateEntity() {
    super.updateEntity()
    if (world.getTotalWorldTime % 10 == 0) {
      // Get a list of all living entities we could possibly detect, using a rough
      // bounding box check, then refining it using the actual distance and an
      // actual visibility check.
      val entities = world.getEntitiesWithinAABB(classOf[EntityLivingBase], sensorBounds)
        .filter(entity => entity.isEntityAlive && isInRange(entity) && isVisible(entity))
        .toSet
      // Get rid of all tracked entities that are no longer visible.
      trackedEntities.retain((key, _) => entities.contains(key))
      // Check for which entities we should generate a signal.
      for (entity <- entities) {
        trackedEntities.get(entity) match {
          case Some((prevX, prevY, prevZ)) =>
            // Known entity, check if it moved enough to trigger.
            if (entity.getDistanceSq(prevX, prevY, prevZ) > sensitivity * sensitivity * 2) {
              sendSignal(entity)
            }
          case _ =>
            // New, unknown entity, always trigger.
            sendSignal(entity)
        }
        // Update tracked position.
        trackedEntities += entity ->(entity.posX, entity.posY, entity.posZ)
      }
    }
  }

  private def sensorBounds = new AxisAlignedBB(
    x + 0.5 - radius, y + 0.5 - radius, z + 0.5 - radius,
    x + 0.5 + radius, y + 0.5 + radius, z + 0.5 + radius)

  private def isInRange(entity: EntityLivingBase) = entity.getDistanceSq(x + 0.5, y + 0.5, z + 0.5) <= radius * radius

  private def isVisible(entity: EntityLivingBase) =
    entity.getActivePotionEffect(Potion.getPotionFromResourceLocation("invisibility")) == null &&
      // Note: it only working in lit conditions works and is neat, but this
      // is pseudo-infrared driven (it only works for *living* entities, after
      // all), so I think it makes more sense for it to work in the dark, too.
      /* entity.getBrightness(0) > 0.2 && */ {
      var ox = x + 0.5
      var oy = y + 0.5
      var oz = z + 0.5
      val target = new Vec3d(entity.posX, entity.posY, entity.posZ)
      // Start trace outside of this block.
      if (entity.posX < x) ox -= 0.75
      if (entity.posX > x + 1) ox += 0.75
      if (entity.posY < y) oy -= 0.75
      if (entity.posY > y + 1) oy += 0.75
      if (entity.posZ < z) oz -= 0.75
      if (entity.posZ > z + 1) oz += 0.75
      world.rayTraceBlocks(new Vec3d(ox, oy, oz), target) == null
    }

  private def sendSignal(entity: EntityLivingBase) {
    if (Settings.get.inputUsername) {
      node.sendToReachable("computer.signal", "motion", Double.box(entity.posX - (x + 0.5)), Double.box(entity.posY - (y + 0.5)), Double.box(entity.posZ - (z + 0.5)), entity.getName)
    }
    else {
      node.sendToReachable("computer.signal", "motion", Double.box(entity.posX - (x + 0.5)), Double.box(entity.posY - (y + 0.5)), Double.box(entity.posZ - (z + 0.5)))
    }
  }

  // ----------------------------------------------------------------------- //

  @Callback(direct = true, doc = """function():number -- Gets the current sensor sensitivity.""")
  def getSensitivity(computer: Context, args: Arguments): Array[AnyRef] = result(sensitivity)

  @Callback(direct = true, doc = """function(value:number):number -- Sets the sensor's sensitivity. Returns the old value.""")
  def setSensitivity(computer: Context, args: Arguments): Array[AnyRef] = {
    val oldValue = sensitivity
    sensitivity = math.max(0.2, args.checkDouble(0))
    result(oldValue)
  }

  // ----------------------------------------------------------------------- //

  override def readFromNBTForServer(nbt: NBTTagCompound) {
    super.readFromNBTForServer(nbt)
    sensitivity = nbt.getDouble(Settings.namespace + "sensitivity")
  }

  override def writeToNBTForServer(nbt: NBTTagCompound) {
    super.writeToNBTForServer(nbt)
    nbt.setDouble(Settings.namespace + "sensitivity", sensitivity)
  }
}
