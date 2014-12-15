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
import li.cil.oc.util.ExtendedArguments._
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.ResultWrapper.result
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityMinecart
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.MovingObjectPosition
import net.minecraft.util.MovingObjectPosition.MovingObjectType
import net.minecraft.util.Vec3
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.common.util.ForgeDirection
import net.minecraftforge.fluids._

import scala.collection.convert.WrapAsScala._

class Robot(val robot: tileentity.Robot) extends prefab.ManagedEnvironment with traits.WorldInspectable with traits.InventoryInspectable with traits.InventoryWorldInterop {
  override val node = api.Network.newNode(this, Visibility.Network).
    withComponent("robot").
    withConnector(Settings.get.bufferRobot).
    create()

  val romRobot = Option(api.FileSystem.asManagedEnvironment(api.FileSystem.
    fromClass(OpenComputers.getClass, Settings.resourceDomain, "lua/component/robot"), "robot"))

  def world = robot.world

  def x = robot.x

  def y = robot.y

  def z = robot.z

  // ----------------------------------------------------------------------- //

  def actualSlot(n: Int) = robot.actualSlot(n)

  val inventory = new IInventory {
    override def getSizeInventory = robot.inventorySize

    override def getInventoryStackLimit = robot.getInventoryStackLimit

    override def markDirty() = robot.markDirty()

    override def isItemValidForSlot(slot: Int, stack: ItemStack) = robot.isItemValidForSlot(actualSlot(slot), stack)

    override def getStackInSlot(slot: Int) = robot.getStackInSlot(actualSlot(slot))

    override def setInventorySlotContents(slot: Int, stack: ItemStack) = robot.setInventorySlotContents(actualSlot(slot), stack)

    override def decrStackSize(slot: Int, amount: Int) = robot.decrStackSize(actualSlot(slot), amount)

    override def getInventoryName = robot.getInventoryName

    override def hasCustomInventoryName = robot.hasCustomInventoryName

    override def openInventory() = robot.openInventory()

    override def closeInventory() = robot.closeInventory()

    override def getStackInSlotOnClosing(slot: Int) = robot.getStackInSlotOnClosing(actualSlot(slot))

    override def isUseableByPlayer(player: EntityPlayer) = robot.isUseableByPlayer(player)
  }

  override def selectedSlot = robot.selectedSlot - actualSlot(0)

  override def selectedSlot_=(value: Int) {
    robot.selectedSlot = value + actualSlot(0)
    ServerPacketSender.sendRobotSelectedSlotChange(robot)
  }

  // ----------------------------------------------------------------------- //

  override protected def fakePlayer = robot.player()

  override protected def checkSideForAction(args: Arguments, n: Int) = robot.toGlobal(args.checkSideForAction(n))

  // ----------------------------------------------------------------------- //

  def selectedTank = robot.selectedTank

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
                if (world.extinguishFire(player, x, y, z, facing.ordinal)) {
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

  // ----------------------------------------------------------------------- //

  @Callback
  def tankCount(context: Context, args: Arguments): Array[AnyRef] = result(robot.tankCount)

  @Callback
  def selectTank(context: Context, args: Arguments): Array[AnyRef] = {
    if (args.count > 0 && args.checkAny(0) != null) {
      robot.selectedTank = checkTank(args, 0)
    }
    result(selectedTank + 1)
  }

  @Callback(direct = true)
  def tankLevel(context: Context, args: Arguments): Array[AnyRef] = {
    val index =
      if (args.count > 0 && args.checkAny(0) != null) checkTank(args, 0)
      else selectedTank
    result(fluidInTank(index) match {
      case Some(fluid) => fluid.amount
      case _ => 0
    })
  }

  @Callback(direct = true)
  def tankSpace(context: Context, args: Arguments): Array[AnyRef] = {
    val index =
      if (args.count > 0 && args.checkAny(0) != null) checkTank(args, 0)
      else selectedTank
    result(getTank(index) match {
      case Some(tank) => tank.getCapacity - tank.getFluidAmount
      case _ => 0
    })
  }

  @Callback
  def compareFluidTo(context: Context, args: Arguments): Array[AnyRef] = {
    val index = checkTank(args, 0)
    result((fluidInTank(selectedTank), fluidInTank(index)) match {
      case (Some(stackA), Some(stackB)) => haveSameFluidType(stackA, stackB)
      case (None, None) => true
      case _ => false
    })
  }

  @Callback
  def transferFluidTo(context: Context, args: Arguments): Array[AnyRef] = {
    val index = checkTank(args, 0)
    val count = args.optionalFluidCount(1)
    if (index == selectedTank || count == 0) {
      result(true)
    }
    else (getTank(selectedTank), getTank(index)) match {
      case (Some(from), Some(to)) =>
        val drained = from.drain(count, false)
        val transferred = to.fill(drained, true)
        if (transferred > 0) {
          from.drain(transferred, true)
          robot.markDirty()
          result(true)
        }
        else if (count >= from.getFluidAmount && to.getCapacity >= from.getFluidAmount && from.getCapacity >= to.getFluidAmount) {
          // Swap.
          val tmp = to.drain(to.getFluidAmount, true)
          to.fill(from.drain(from.getFluidAmount, true), true)
          from.fill(tmp, true)
          robot.markDirty()
          result(true)
        }
        else result(Unit, "incompatible or no fluid")
      case _ => result(Unit, "invalid index")
    }
  }

  @Callback
  def compareFluid(context: Context, args: Arguments): Array[AnyRef] = {
    val side = checkSideForAction(args, 0)
    fluidInTank(selectedTank) match {
      case Some(stack) =>
        val (nx, ny, nz) = (x + side.offsetX, y + side.offsetY, z + side.offsetZ)
        if (world.blockExists(nx, ny, nz)) world.getTileEntity(nx, ny, nz) match {
          case handler: IFluidHandler =>
            result(Option(handler.getTankInfo(side.getOpposite)).exists(_.exists(other => stack.isFluidEqual(other.fluid))))
          case _ =>
            val block = world.getBlock(x + side.offsetX, y + side.offsetY, z + side.offsetZ)
            val fluid = FluidRegistry.lookupFluidForBlock(block)
            result(stack.getFluid == fluid)
        }
        else result(false)
      case _ => result(false)
    }
  }

  @Callback
  def drain(context: Context, args: Arguments): Array[AnyRef] = {
    val facing = checkSideForAction(args, 0)
    val count = args.optionalFluidCount(1)
    getTank(selectedTank) match {
      case Some(tank) =>
        val space = tank.getCapacity - tank.getFluidAmount
        val amount = math.min(count, space)
        if (count > 0 && amount == 0) {
          result(Unit, "tank is full")
        }
        else {
          val (nx, ny, nz) = (x + facing.offsetX, y + facing.offsetY, z + facing.offsetZ)
          if (world.blockExists(nx, ny, nz)) world.getTileEntity(nx, ny, nz) match {
            case handler: IFluidHandler =>
              tank.getFluid match {
                case stack: FluidStack =>
                  val drained = handler.drain(facing.getOpposite, new FluidStack(stack, amount), true)
                  if ((drained != null && drained.amount > 0) || amount == 0) {
                    tank.fill(drained, true)
                    result(true)
                  }
                  else result(Unit, "incompatible or no fluid")
                case _ =>
                  tank.fill(handler.drain(facing.getOpposite, amount, true), true)
                  result(true)
              }
            case _ =>
              val block = world.getBlock(x + facing.offsetX, y + facing.offsetY, z + facing.offsetZ)
              val fluid = FluidRegistry.lookupFluidForBlock(block)
              if (fluid == null) {
                result(Unit, "incompatible or no fluid")
              }
              else if (tank.fill(new FluidStack(fluid, 1000), false) == 1000) {
                tank.fill(new FluidStack(fluid, 1000), true)
                world.setBlockToAir(x + facing.offsetX, y + facing.offsetY, z + facing.offsetZ)
                result(true)
              }
              else result(Unit, "tank is full")
          }
          else result(Unit, "incompatible or no fluid")
        }
      case _ => result(Unit, "no tank selected")
    }
  }

  @Callback
  def fill(context: Context, args: Arguments): Array[AnyRef] = {
    val facing = checkSideForAction(args, 0)
    val count = args.optionalFluidCount(1)
    getTank(selectedTank) match {
      case Some(tank) =>
        val amount = math.min(count, tank.getFluidAmount)
        if (count > 0 && amount == 0) {
          result(Unit, "tank is empty")
        }
        val (bx, by, bz) = (x + facing.offsetX, y + facing.offsetY, z + facing.offsetZ)
        if (world.blockExists(bx, by, bz)) world.getTileEntity(bx, by, bz) match {
          case handler: IFluidHandler =>
            tank.getFluid match {
              case stack: FluidStack =>
                val filled = handler.fill(facing.getOpposite, new FluidStack(stack, amount), true)
                if (filled > 0 || amount == 0) {
                  tank.drain(filled, true)
                  result(true)
                }
                else result(Unit, "incompatible or no fluid")
              case _ =>
                result(Unit, "tank is empty")
            }
          case _ =>
            val block = world.getBlock(bx, by, bz)
            if (block != null && !block.isAir(world, x, y, z) && !block.isReplaceable(world, x, y, z)) {
              result(Unit, "no space")
            }
            else if (tank.getFluidAmount < 1000) {
              result(Unit, "tank is empty")
            }
            else if (!tank.getFluid.getFluid.canBePlacedInWorld) {
              result(Unit, "incompatible fluid")
            }
            else {
              val fluidBlock = tank.getFluid.getFluid.getBlock
              tank.drain(1000, true)
              world.func_147480_a(bx, by, bz, true)
              world.setBlock(bx, by, bz, fluidBlock)
              // This fake neighbor update is required to get stills to start flowing.
              world.notifyBlockOfNeighborChange(bx, by, bz, robot.block)
              result(true)
            }
        }
        else result(Unit, "no space")
      case _ => result(Unit, "no tank selected")
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
    (x, y, z,
      0.5f + facing.offsetX * 0.5f,
      0.5f + facing.offsetY * 0.5f,
      0.5f + facing.offsetZ * 0.5f)
  }

  private def clickParamsForItemUse(facing: ForgeDirection, side: ForgeDirection) = {
    (x + facing.offsetX + side.offsetX,
      y + facing.offsetY + side.offsetY,
      z + facing.offsetZ + side.offsetZ,
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

  // ----------------------------------------------------------------------- //

  private def haveSameFluidType(stackA: FluidStack, stackB: FluidStack) = stackA.isFluidEqual(stackB)

  private def getTank(index: Int) = robot.tryGetTank(index)

  private def fluidInTank(index: Int) = getTank(index) match {
    case Some(tank) => Option(tank.getFluid)
    case _ => None
  }

  // ----------------------------------------------------------------------- //

  private def checkTank(args: Arguments, n: Int) = args.checkTank(robot, n)

  private def checkSideForFace(args: Arguments, n: Int, facing: ForgeDirection) = robot.toGlobal(args.checkSideForFace(n, robot.toLocal(facing)))
}
