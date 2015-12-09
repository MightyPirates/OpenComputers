package li.cil.oc.server.component

import li.cil.oc.Settings
import li.cil.oc.api.Network
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network.Visibility
import li.cil.oc.api.prefab
import li.cil.oc.common.entity
import li.cil.oc.util.ExtendedArguments._
import li.cil.oc.util.InventoryUtils
import net.minecraft.entity.item.EntityItem
import net.minecraft.util.EnumFacing

import scala.collection.convert.WrapAsJava._
import scala.collection.convert.WrapAsScala._

class Drone(val agent: entity.Drone) extends prefab.ManagedEnvironment with Agent {
  override val node = Network.newNode(this, Visibility.Network).
    withComponent("drone").
    withConnector(Settings.get.bufferDrone).
    create()

  override protected def checkSideForAction(args: Arguments, n: Int) =
    args.checkSideAny(n)

  override protected def suckableItems(side: EnumFacing) = entitiesInBlock(classOf[EntityItem], position) ++ super.suckableItems(side)

  override protected def onSuckCollect(entity: EntityItem) = {
    if (InventoryUtils.insertIntoInventory(entity.getEntityItem, inventory, slots = Option(insertionSlots))) {
      world.playSoundAtEntity(agent, "random.pop", 0.2f, ((world.rand.nextFloat - world.rand.nextFloat) * 0.7f + 1) * 2)
    }
  }

  override def onWorldInteraction(context: Context, duration: Double): Unit = {
    super.onWorldInteraction(context, duration * 2)
  }

  // ----------------------------------------------------------------------- //

  @Callback(doc = "function():string -- Get the status text currently being displayed in the GUI.")
  def getStatusText(context: Context, args: Arguments): Array[AnyRef] = result(agent.statusText)

  @Callback(doc = "function(value:string):string -- Set the status text to display in the GUI, returns new value.")
  def setStatusText(context: Context, args: Arguments): Array[AnyRef] = {
    agent.statusText = args.checkString(0)
    context.pause(0.1)
    result(agent.statusText)
  }

  @Callback(doc = "function():number -- Get the current color of the flap lights as an integer encoded RGB value (0xRRGGBB).")
  def getLightColor(context: Context, args: Arguments): Array[AnyRef] = result(agent.lightColor)

  @Callback(doc = "function(value:number):number -- Set the color of the flap lights to the specified integer encoded RGB value (0xRRGGBB).")
  def setLightColor(context: Context, args: Arguments): Array[AnyRef] = {
    agent.lightColor = args.checkInteger(0)
    context.pause(0.1)
    result(agent.lightColor)
  }

  // ----------------------------------------------------------------------- //

  @Callback(doc = "function(dx:number, dy:number, dz:number) -- Change the target position by the specified offset.")
  def move(context: Context, args: Arguments): Array[AnyRef] = {
    val dx = args.checkDouble(0).toFloat
    val dy = args.checkDouble(1).toFloat
    val dz = args.checkDouble(2).toFloat
    agent.targetX += dx
    agent.targetY += dy
    agent.targetZ += dz
    null
  }

  @Callback(doc = "function():number -- Get the current distance to the target position.")
  def getOffset(context: Context, args: Arguments): Array[AnyRef] =
    result(agent.getDistance(agent.targetX, agent.targetY, agent.targetZ))

  @Callback(doc = "function():number -- Get the current velocity in m/s.")
  def getVelocity(context: Context, args: Arguments): Array[AnyRef] =
    result(math.sqrt(agent.motionX * agent.motionX + agent.motionY * agent.motionY + agent.motionZ * agent.motionZ) * 20) // per second

  @Callback(doc = "function():number -- Get the maximum velocity, in m/s.")
  def getMaxVelocity(context: Context, args: Arguments): Array[AnyRef] = {
    result(agent.maxVelocity * 20) // per second
  }

  @Callback(doc = "function():number -- Get the currently set acceleration.")
  def getAcceleration(context: Context, args: Arguments): Array[AnyRef] = {
    result(agent.targetAcceleration * 20) // per second
  }

  @Callback(doc = "function(value:number):number -- Try to set the acceleration to the specified value and return the new acceleration.")
  def setAcceleration(context: Context, args: Arguments): Array[AnyRef] = {
    agent.targetAcceleration = (args.checkDouble(0) / 20.0).toFloat
    result(agent.targetAcceleration * 20)
  }
}
