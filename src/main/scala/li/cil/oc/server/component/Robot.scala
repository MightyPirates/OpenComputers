package li.cil.oc.server.component

import java.util

import li.cil.oc.Constants
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute
import li.cil.oc.api.driver.DeviceInfo.DeviceClass
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.driver.DeviceInfo
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network._
import li.cil.oc.api.prefab
import li.cil.oc.api.prefab.AbstractManagedEnvironment
import li.cil.oc.common.ToolDurabilityProviders
import li.cil.oc.common.tileentity
import li.cil.oc.server.PacketSender
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedArguments._
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.StackOption
import li.cil.oc.util.StackOption._
import net.minecraft.nbt.CompoundNBT
import net.minecraft.particles.ParticleTypes
import net.minecraft.util.Direction

import scala.collection.convert.ImplicitConversionsToJava._

class Robot(val agent: tileentity.Robot) extends AbstractManagedEnvironment with Agent with DeviceInfo {
  override val node = api.Network.newNode(this, Visibility.Network).
    withComponent("robot").
    withConnector(Settings.get.bufferRobot).
    create()

  val romRobot = Option(api.FileSystem.asManagedEnvironment(api.FileSystem.
    fromClass(OpenComputers.getClass, Settings.resourceDomain, "lua/component/robot"), "robot"))

  private final lazy val deviceInfo = Map(
    DeviceAttribute.Class -> DeviceClass.System,
    DeviceAttribute.Description -> "Robot",
    DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
    DeviceAttribute.Product -> "Caterpillar",
    DeviceAttribute.Capacity -> agent.getContainerSize.toString
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
    StackOption(agent.equipmentInventory.getItem(0)) match {
      case SomeStack(item) =>
        ToolDurabilityProviders.getDurability(item) match {
          case Some(durability) => result(durability)
          case _ => result(Unit, "tool cannot be damaged")
        }
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
        PacketSender.sendParticleEffect(BlockPosition(agent), ParticleTypes.CRIT, 8, 0.25, Some(direction))
        result(Unit, what)
      }
      else {
        if (!node.tryChangeBuffer(-Settings.get.robotMoveCost)) {
          result(Unit, "not enough energy")
        }
        else if (agent.move(direction)) {
          context.pause(Settings.get.moveDelay)
          result(true)
        }
        else {
          node.changeBuffer(Settings.get.robotMoveCost)
          context.pause(0.4)
          PacketSender.sendParticleEffect(BlockPosition(agent), ParticleTypes.CRIT, 8, 0.25, Some(direction))
          result(Unit, "impossible move")
        }
      }
    }
  }

  @Callback(doc = "function(clockwise:boolean):boolean -- Rotate in the specified direction.")
  def turn(context: Context, args: Arguments): Array[AnyRef] = {
    val clockwise = args.checkBoolean(0)
    if (node.tryChangeBuffer(-Settings.get.robotTurnCost)) {
      if (clockwise) agent.rotate(Direction.UP)
      else agent.rotate(Direction.DOWN)
      agent.animateTurn(clockwise, Settings.get.turnDelay)
      context.pause(Settings.get.turnDelay)
      result(true)
    }
    else {
      result(Unit, "not enough energy")
    }
  }

  // ----------------------------------------------------------------------- //

  override def onConnect(node: Node) {
    super.onConnect(node)
    if (node == this.node) {
      romRobot.foreach(fs => {
        fs.node.asInstanceOf[Component].setVisibility(Visibility.Network)
        node.connect(fs.node)
      })
    }
  }

  override def onMessage(message: Message) {
    super.onMessage(message)
    if (message.name == "network.message" && message.source != agent.node) message.data match {
      case Array(packet: Packet) => agent.proxy.node.sendToReachable(message.name, packet)
      case _ =>
    }
  }

  // ----------------------------------------------------------------------- //

  private final val RomRobotTag = "romRobot"

  override def loadData(nbt: CompoundNBT) {
    super.loadData(nbt)
    romRobot.foreach(_.loadData(nbt.getCompound(RomRobotTag)))
  }

  override def saveData(nbt: CompoundNBT) {
    super.saveData(nbt)
    romRobot.foreach(fs => nbt.setNewCompoundTag(RomRobotTag, fs.saveData))
  }
}
