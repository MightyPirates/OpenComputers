package li.cil.oc.server.component

import java.util

import li.cil.oc.Constants
import li.cil.oc.Settings
import li.cil.oc.api.Network
import li.cil.oc.api.driver.DeviceInfo
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute
import li.cil.oc.api.driver.DeviceInfo.DeviceClass
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network.Visibility
import li.cil.oc.api.prefab
import li.cil.oc.api.prefab.AbstractManagedEnvironment
import li.cil.oc.common.entity
import li.cil.oc.util.ExtendedArguments._
import li.cil.oc.util.InventoryUtils
import net.minecraft.entity.item.ItemEntity
import net.minecraft.util.SoundEvents
import net.minecraft.util.Direction
import net.minecraft.util.SoundCategory

import scala.collection.convert.ImplicitConversionsToJava._
import scala.collection.convert.ImplicitConversionsToScala._

class Drone(val agent: entity.Drone) extends AbstractManagedEnvironment with Agent with DeviceInfo {
  override val node = Network.newNode(this, Visibility.Network).
    withComponent("drone").
    withConnector(Settings.get.bufferDrone).
    create()

  private final lazy val deviceInfo = Map(
    DeviceAttribute.Class -> DeviceClass.System,
    DeviceAttribute.Description -> "Drone",
    DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
    DeviceAttribute.Product -> "Overwatcher",
    DeviceAttribute.Capacity -> agent.inventorySize.toString
  )

  override def getDeviceInfo: util.Map[String, String] = deviceInfo

  override protected def checkSideForAction(args: Arguments, n: Int) =
    args.checkSideAny(n)

  override protected def suckableItems(side: Direction) = entitiesInBlock(classOf[ItemEntity], position) ++ super.suckableItems(side)

  override protected def onSuckCollect(entity: ItemEntity) = {
    if (InventoryUtils.insertIntoInventory(entity.getItem, InventoryUtils.asItemHandler(inventory), slots = Option(insertionSlots))) {
      world.playSound(agent.player, agent.getX, agent.getY, agent.getZ, SoundEvents.ITEM_PICKUP, SoundCategory.NEUTRAL, 0.2f, ((world.random.nextFloat - world.random.nextFloat) * 0.7f + 1) * 2)
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
    result(agent.position.distanceTo(agent.getTarget()))

  @Callback(doc = "function():number -- Get the current velocity in m/s.")
  def getVelocity(context: Context, args: Arguments): Array[AnyRef] =
    result(math.sqrt(agent.motionX * agent.motionX + agent.motionY * agent.motionY + agent.motionZ * agent.motionZ) * 20) // per second

  @Callback(doc = "function():number -- Get the maximum velocity, in m/s.")
  def getV1elocity(context: Context, args: Arguments): Array[AnyRef] = {
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
