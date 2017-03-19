package li.cil.oc.server.component

import java.util

import li.cil.oc.{Constants, OpenComputers, Settings, api}
import li.cil.oc.api.driver.DeviceInfo
import li.cil.oc.api.driver.DeviceInfo.{DeviceAttribute, DeviceClass}
import li.cil.oc.api.machine.{Arguments, Callback, Context}
import li.cil.oc.api.network._
import li.cil.oc.api.prefab.network.AbstractManagedNodeContainer
import li.cil.oc.common.{ToolDurabilityProviders, tileentity}
import li.cil.oc.server.PacketSender
import li.cil.oc.util.ExtendedArguments._
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.{EnumFacing, EnumParticleTypes}

import scala.collection.convert.WrapAsJava._

class Robot(val agent: tileentity.Robot) extends AbstractManagedNodeContainer with Agent with DeviceInfo {
  override val getNode = api.Network.newNode(this, Visibility.NETWORK).
    withComponent("robot").
    withConnector(Settings.Power.Buffer.robot).
    create()

  val romRobot = Option(api.FileSystem.asManagedEnvironment(api.FileSystem.
    fromClass(OpenComputers.getClass, Constants.resourceDomain, "lua/component/robot"), "robot"))

  private final lazy val deviceInfo = Map(
    DeviceAttribute.Class -> DeviceClass.System,
    DeviceAttribute.Description -> "Robot",
    DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
    DeviceAttribute.Product -> "Caterpillar",
    DeviceAttribute.Capacity -> agent.getSizeInventory.toString
  )

  override def getDeviceInfo: util.Map[String, String] = deviceInfo

  // ----------------------------------------------------------------------- //

  override protected def checkSideForAction(args: Arguments, n: Int) = agent.toGlobal(args.checkSideForAction(n))

  override def onWorldInteraction(context: Context, duration: Double): Unit = {
    super.onWorldInteraction(context, duration)
    agent.animateSwing(duration)
  }

  // ----------------------------------------------------------------------- //

  @Callback(doc = "function():number -- Get the current color of the activity light as an integer encoded RGB value (0xRRGGBB).")
  def getLightColor(context: Context, args: Arguments): Array[AnyRef] = result(agent.info.lightColor)

  @Callback(doc = "function(value:number):number -- Set the color of the activity light to the specified integer encoded RGB value (0xRRGGBB).")
  def setLightColor(context: Context, args: Arguments): Array[AnyRef] = {
    agent.setLightColor(args.checkInteger(0))
    context.pause(0.1)
    result(agent.info.lightColor)
  }

  // ----------------------------------------------------------------------- //

  @Callback(doc = "function():number -- Get the durability of the currently equipped tool.")
  def durability(context: Context, args: Arguments): Array[AnyRef] = {
    Option(agent.equipmentInventory.getStackInSlot(0)) match {
      case Some(item) =>
        val durability = ToolDurabilityProviders.getDurability(item)
        if (!Double.isNaN(durability)) result(durability)
        else result(Unit, "tool cannot be damaged")
      case _ => result(Unit, "no tool equipped")
    }
  }

  // ----------------------------------------------------------------------- //

  @Callback(doc = "function(direction:number):boolean -- Move in the specified direction.")
  def move(context: Context, args: Arguments): Array[AnyRef] = {
    val direction = agent.toGlobal(args.checkSideForMovement(0))
    if (agent.isAnimatingMove) {
      // This shouldn't really happen due to delays being enforced, but just to
      // be on the safe side...
      result(Unit, "already moving")
    }
    else {
      val (something, what) = blockContent(direction)
      if (something) {
        context.pause(0.4)
        PacketSender.sendParticleEffect(BlockPosition(agent), EnumParticleTypes.CRIT, 8, 0.25, Some(direction))
        result(Unit, what)
      }
      else {
        if (!getNode.tryChangeEnergy(-Settings.Power.Cost.robotMove)) {
          result(Unit, "not enough energy")
        }
        else if (agent.move(direction)) {
          val delay = Settings.Robot.Delays.skipCurrentTick(Settings.Robot.Delays.move)
          context.pause(delay)
          result(true)
        }
        else {
          getNode.changeEnergy(Settings.Power.Cost.robotMove)
          context.pause(0.4)
          PacketSender.sendParticleEffect(BlockPosition(agent), EnumParticleTypes.CRIT, 8, 0.25, Some(direction))
          result(Unit, "impossible move")
        }
      }
    }
  }

  @Callback(doc = "function(clockwise:boolean):boolean -- Rotate in the specified direction.")
  def turn(context: Context, args: Arguments): Array[AnyRef] = {
    val clockwise = args.checkBoolean(0)
    if (getNode.tryChangeEnergy(-Settings.Power.Cost.robotTurn)) {
      if (clockwise) agent.rotate(EnumFacing.UP)
      else agent.rotate(EnumFacing.DOWN)
      val delay = Settings.Robot.Delays.skipCurrentTick(Settings.Robot.Delays.turn)
      agent.animateTurn(clockwise, delay)
      context.pause(delay)
      result(true)
    }
    else {
      result(Unit, "not enough energy")
    }
  }

  // ----------------------------------------------------------------------- //

  override def onConnect(node: Node) {
    super.onConnect(node)
    if (node == this.getNode) {
      romRobot.foreach(fs => {
        fs.getNode.asInstanceOf[ComponentNode].setVisibility(Visibility.NETWORK)
        node.connect(fs.getNode)
      })
    }
  }

  override def onMessage(message: Message) {
    super.onMessage(message)
    if (message.getName == "network.message" && message.getSource != agent.getNode) message.getData match {
      case Array(packet: Packet) => agent.proxy.getNode.sendToReachable(message.getName, packet)
      case _ =>
    }
  }

  // ----------------------------------------------------------------------- //

  private final val RomRobotTag = "romRobot"

  override def load(nbt: NBTTagCompound) {
    super.load(nbt)
    romRobot.foreach(_.load(nbt.getCompoundTag(RomRobotTag)))
  }

  override def save(nbt: NBTTagCompound) {
    super.save(nbt)
    romRobot.foreach(fs => nbt.setNewCompoundTag(RomRobotTag, fs.save))
  }
}
