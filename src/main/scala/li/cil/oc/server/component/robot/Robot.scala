package li.cil.oc.server.component.robot

import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.event.RobotPlaceInAirEvent
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network._
import li.cil.oc.api.prefab
import li.cil.oc.common.ToolDurabilityProviders
import li.cil.oc.common.tileentity
import li.cil.oc.server.component.traits
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedArguments._
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.ExtendedWorld._
import li.cil.oc.util.ResultWrapper.result
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityMinecart
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.MovingObjectPosition
import net.minecraft.util.MovingObjectPosition.MovingObjectType
import net.minecraft.util.Vec3
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.common.util.ForgeDirection

import scala.collection.convert.WrapAsScala._

class Robot(val robot: tileentity.Robot) extends prefab.ManagedEnvironment with traits.WorldControl with traits.InventoryControl with traits.InventoryWorldControl with traits.TankControl with traits.TankWorldControl {
  override val node = api.Network.newNode(this, Visibility.Network).
    withComponent("robot").
    withConnector(Settings.get.bufferRobot).
    create()

  val romRobot = Option(api.FileSystem.asManagedEnvironment(api.FileSystem.
    fromClass(OpenComputers.getClass, Settings.resourceDomain, "lua/component/robot"), "robot"))

  override def position = BlockPosition(robot)

  // ----------------------------------------------------------------------- //

  def actualSlot(n: Int) = robot.actualSlot(n)

  override def inventory = robot.dynamicInventory

  override def selectedSlot = robot.selectedSlot - actualSlot(0)

  override def selectedSlot_=(value: Int) {
    robot.selectedSlot = value + actualSlot(0)
    ServerPacketSender.sendRobotSelectedSlotChange(robot)
  }

  // ----------------------------------------------------------------------- //

  override def tank = robot.tank

  def selectedTank = robot.selectedTank

  override def selectedTank_=(value: Int) = robot.selectedTank = value

  // ----------------------------------------------------------------------- //

  override def fakePlayer = robot.player()

  override protected def checkSideForAction(args: Arguments, n: Int) = robot.toGlobal(args.checkSideForAction(n))

  private def checkSideForFace(args: Arguments, n: Int, facing: ForgeDirection) = robot.toGlobal(args.checkSideForFace(n, robot.toLocal(facing)))

  // ----------------------------------------------------------------------- //

  def canPlaceInAir = {
    val event = new RobotPlaceInAirEvent(robot)
    MinecraftForge.EVENT_BUS.post(event)
    event.isAllowed
  }

  // ----------------------------------------------------------------------- //

  @Callback
  def name(context: Context, args: Arguments): Array[AnyRef] = result(robot.name)

  // ----------------------------------------------------------------------- //

  @Callback
  def place(context: Context, args: Arguments): Array[AnyRef] = {
    val facing = checkSideForAction(args, 0)
    val sides =
      if (args.isInteger(1)) {
        Iterable(checkSideForFace(args, 1, facing))
      }
      else {
        // Always try the direction we're looking first.
        Iterable(facing) ++ ForgeDirection.VALID_DIRECTIONS.filter(side => side != facing && side != facing.getOpposite).toIterable
      }
    val sneaky = args.isBoolean(2) && args.checkBoolean(2)
    val stack = robot.inventory.selectedItemStack
    if (stack == null || stack.stackSize == 0) {
      return result(Unit, "nothing selected")
    }

    for (side <- sides) {
      val player = robot.player(facing, side)
      player.setSneaking(sneaky)
      val success = Option(pick(player, Settings.get.useAndPlaceRange)) match {
        case Some(hit) if hit.typeOfHit == MovingObjectType.BLOCK =>
          val (bx, by, bz, hx, hy, hz) = clickParamsFromHit(hit)
          player.placeBlock(robot.selectedSlot, bx, by, bz, hit.sideHit, hx, hy, hz)
        case None if canPlaceInAir && player.closestEntity[Entity]().isEmpty =>
          val (bx, by, bz, hx, hy, hz) = clickParamsForPlace(facing)
          player.placeBlock(robot.selectedSlot, bx, by, bz, facing.ordinal, hx, hy, hz)
        case _ => false
      }
      player.setSneaking(false)
      if (success) {
        context.pause(Settings.get.placeDelay)
        robot.animateSwing(Settings.get.placeDelay)
        return result(true)
      }
    }

    result(false)
  }

  // ----------------------------------------------------------------------- //

  @Callback
  def swing(context: Context, args: Arguments): Array[AnyRef] = {
    // Swing the equipped tool (left click).
    val facing = checkSideForAction(args, 0)
    val sides =
      if (args.isInteger(1)) {
        Iterable(checkSideForFace(args, 1, facing))
      }
      else {
        // Always try the direction we're looking first.
        Iterable(facing) ++ ForgeDirection.VALID_DIRECTIONS.filter(side => side != facing && side != facing.getOpposite).toIterable
      }
    val sneaky = args.isBoolean(2) && args.checkBoolean(2)

    def triggerDelay(delay: Double = Settings.get.swingDelay) = {
      context.pause(delay)
      robot.animateSwing(Settings.get.swingDelay)
    }
    def attack(player: Player, entity: Entity) = {
      beginConsumeDrops(entity)
      player.attackTargetEntityWithCurrentItem(entity)
      // Mine carts have to be hit quickly in succession to break, so we click
      // until it breaks. But avoid an infinite loop... you never know.
      entity match {
        case _: EntityMinecart => for (_ <- 0 until 10 if !entity.isDead) {
          player.attackTargetEntityWithCurrentItem(entity)
        }
        case _ =>
      }
      endConsumeDrops(player, entity)
      triggerDelay()
      (true, "entity")
    }
    def click(player: Player, x: Int, y: Int, z: Int, side: Int) = {
      val breakTime = player.clickBlock(x, y, z, side)
      val broke = breakTime > 0
      if (broke) {
        // Subtract one tick because we take one to trigger the action - a bit
        // more than one tick avoid floating point inaccuracy incurring another
        // tick of delay.
        triggerDelay(breakTime - 0.055)
      }
      (broke, "block")
    }

    var reason: Option[String] = None
    for (side <- sides) {
      val player = robot.player(facing, side)
      player.setSneaking(sneaky)

      val (success, what) = {
        val hit = pick(player, Settings.get.swingRange)
        (Option(hit) match {
          case Some(info) => info.typeOfHit
          case _ => MovingObjectType.MISS
        }) match {
          case MovingObjectType.ENTITY =>
            attack(player, hit.entityHit)
          case MovingObjectType.BLOCK =>
            click(player, hit.blockX, hit.blockY, hit.blockZ, hit.sideHit)
          case _ =>
            // Retry with full block bounds, disregarding swing range.
            player.closestEntity[EntityLivingBase]() match {
              case Some(entity) =>
                attack(player, entity)
              case _ =>
                if (world.extinguishFire(player, position, facing)) {
                  triggerDelay()
                  (true, "fire")
                }
                else (false, "air")
            }
        }
      }

      player.setSneaking(false)
      if (success) {
        return result(true, what)
      }
      reason = reason.orElse(Option(what))
    }

    result(false, reason.orNull)
  }

  @Callback
  def use(context: Context, args: Arguments): Array[AnyRef] = {
    val facing = checkSideForAction(args, 0)
    val sides =
      if (args.isInteger(1)) {
        Iterable(checkSideForFace(args, 1, facing))
      }
      else {
        // Always try the direction we're looking first.
        Iterable(facing) ++ ForgeDirection.VALID_DIRECTIONS.filter(side => side != facing && side != facing.getOpposite).toIterable
      }
    val sneaky = args.isBoolean(2) && args.checkBoolean(2)
    val duration =
      if (args.isDouble(3)) args.checkDouble(3)
      else 0.0

    def triggerDelay() {
      context.pause(Settings.get.useDelay)
      robot.animateSwing(Settings.get.useDelay)
    }
    def activationResult(activationType: ActivationType.Value) =
      activationType match {
        case ActivationType.BlockActivated =>
          triggerDelay()
          (true, "block_activated")
        case ActivationType.ItemPlaced =>
          triggerDelay()
          (true, "item_placed")
        case ActivationType.ItemUsed =>
          triggerDelay()
          (true, "item_used")
        case _ => (false, "")
      }
    def interact(player: Player, entity: Entity) = {
      beginConsumeDrops(entity)
      val result = player.interactWith(entity)
      endConsumeDrops(player, entity)
      result
    }

    for (side <- sides) {
      val player = robot.player(facing, side)
      player.setSneaking(sneaky)

      val (success, what) = Option(pick(player, Settings.get.useAndPlaceRange)) match {
        case Some(hit) if hit.typeOfHit == MovingObjectType.ENTITY && interact(player, hit.entityHit) =>
          triggerDelay()
          (true, "item_interacted")
        case Some(hit) if hit.typeOfHit == MovingObjectType.BLOCK =>
          val (bx, by, bz, hx, hy, hz) = clickParamsFromHit(hit)
          activationResult(player.activateBlockOrUseItem(bx, by, bz, hit.sideHit, hx, hy, hz, duration))
        case _ =>
          (if (canPlaceInAir) {
            val (bx, by, bz, hx, hy, hz) = clickParamsForPlace(facing)
            if (player.placeBlock(0, bx, by, bz, facing.ordinal, hx, hy, hz))
              ActivationType.ItemPlaced
            else {
              val (bx, by, bz, hx, hy, hz) = clickParamsForItemUse(facing, side)
              player.activateBlockOrUseItem(bx, by, bz, side.getOpposite.ordinal, hx, hy, hz, duration)
            }
          } else ActivationType.None) match {
            case ActivationType.None =>
              if (player.useEquippedItem(duration)) {
                triggerDelay()
                (true, "item_used")
              }
              else (false, "air")
            case activationType => activationResult(activationType)
          }
      }

      player.setSneaking(false)
      if (success) {
        return result(true, what)
      }
    }

    result(false)
  }

  @Callback
  def durability(context: Context, args: Arguments): Array[AnyRef] = {
    Option(robot.getStackInSlot(0)) match {
      case Some(item) =>
        ToolDurabilityProviders.getDurability(item) match {
          case Some(durability) => result(durability)
          case _ => result(Unit, "tool cannot be damaged")
        }
      case _ => result(Unit, "no tool equipped")
    }
  }

  // ----------------------------------------------------------------------- //

  @Callback
  def move(context: Context, args: Arguments): Array[AnyRef] = {
    val direction = robot.toGlobal(args.checkSideForMovement(0))
    if (robot.isAnimatingMove) {
      // This shouldn't really happen due to delays being enforced, but just to
      // be on the safe side...
      result(Unit, "already moving")
    }
    else {
      val (something, what) = blockContent(direction)
      if (something) {
        result(Unit, what)
      }
      else {
        if (!node.tryChangeBuffer(-Settings.get.robotMoveCost)) {
          result(Unit, "not enough energy")
        }
        else if (robot.move(direction)) {
          context.pause(Settings.get.moveDelay)
          result(true)
        }
        else {
          node.changeBuffer(Settings.get.robotMoveCost)
          result(Unit, "impossible move")
        }
      }
    }
  }

  @Callback
  def turn(context: Context, args: Arguments): Array[AnyRef] = {
    val clockwise = args.checkBoolean(0)
    if (node.tryChangeBuffer(-Settings.get.robotTurnCost)) {
      if (clockwise) robot.rotate(ForgeDirection.UP)
      else robot.rotate(ForgeDirection.DOWN)
      robot.animateTurn(clockwise, Settings.get.turnDelay)
      context.pause(Settings.get.turnDelay)
      result(true)
    }
    else {
      result(Unit, "not enough energy")
    }
  }

  @Callback(doc = "function():number -- Get the current color of the activity light as an integer encoded RGB value (0xRRGGBB).")
  def getLightColor(context: Context, args: Arguments): Array[AnyRef] = result(robot.info.lightColor)

  @Callback(doc = "function(value:number):number -- Set the color of the activity light to the specified integer encoded RGB value (0xRRGGBB).")
  def setLightColor(context: Context, args: Arguments): Array[AnyRef] = {
    robot.setLightColor(args.checkInteger(0))
    context.pause(0.1)
    result(robot.info.lightColor)
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
    if (message.name == "network.message" && message.source != robot.node) message.data match {
      case Array(packet: Packet) => robot.proxy.node.sendToReachable(message.name, packet)
      case _ =>
    }
  }

  // ----------------------------------------------------------------------- //

  override def load(nbt: NBTTagCompound) {
    super.load(nbt)
    romRobot.foreach(_.load(nbt.getCompoundTag("romRobot")))
  }

  override def save(nbt: NBTTagCompound) {
    super.save(nbt)
    romRobot.foreach(fs => nbt.setNewCompoundTag("romRobot", fs.save))
  }

  // ----------------------------------------------------------------------- //

  private def beginConsumeDrops(entity: Entity) {
    entity.captureDrops = true
  }

  private def endConsumeDrops(player: Player, entity: Entity) {
    entity.captureDrops = false
    for (drop <- entity.capturedDrops) {
      val stack = drop.getEntityItem
      player.inventory.addItemStackToInventory(stack)
      if (stack.stackSize > 0) {
        player.dropPlayerItemWithRandomChoice(stack, inPlace = false)
      }
    }
    entity.capturedDrops.clear()
  }

  // ----------------------------------------------------------------------- //

  private def pick(player: Player, range: Double) = {
    val origin = Vec3.createVectorHelper(
      player.posX + player.facing.offsetX * 0.5,
      player.posY + player.facing.offsetY * 0.5,
      player.posZ + player.facing.offsetZ * 0.5)
    val blockCenter = origin.addVector(
      player.facing.offsetX * 0.5,
      player.facing.offsetY * 0.5,
      player.facing.offsetZ * 0.5)
    val target = blockCenter.addVector(
      player.side.offsetX * range,
      player.side.offsetY * range,
      player.side.offsetZ * range)
    val hit = world.rayTraceBlocks(origin, target)
    player.closestEntity[Entity]() match {
      case Some(entity@(_: EntityLivingBase | _: EntityMinecart)) if hit == null || Vec3.createVectorHelper(player.posX, player.posY, player.posZ).distanceTo(hit.hitVec) > player.getDistanceToEntity(entity) => new MovingObjectPosition(entity)
      case _ => hit
    }
  }

  private def clickParamsForPlace(facing: ForgeDirection) = {
    (position.x, position.y, position.z,
      0.5f + facing.offsetX * 0.5f,
      0.5f + facing.offsetY * 0.5f,
      0.5f + facing.offsetZ * 0.5f)
  }

  private def clickParamsForItemUse(facing: ForgeDirection, side: ForgeDirection) = {
    val blockPos = position.offset(facing).offset(side)
    (blockPos.x, blockPos.y, blockPos.z,
      0.5f - side.offsetX * 0.5f,
      0.5f - side.offsetY * 0.5f,
      0.5f - side.offsetZ * 0.5f)
  }

  private def clickParamsFromHit(hit: MovingObjectPosition) = {
    (hit.blockX, hit.blockY, hit.blockZ,
      (hit.hitVec.xCoord - hit.blockX).toFloat,
      (hit.hitVec.yCoord - hit.blockY).toFloat,
      (hit.hitVec.zCoord - hit.blockZ).toFloat)
  }
}
