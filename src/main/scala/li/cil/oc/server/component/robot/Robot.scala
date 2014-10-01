package li.cil.oc.server.component.robot

import li.cil.oc.api.event.RobotPlaceInAirEvent
import li.cil.oc.api.network._
import li.cil.oc.common.component.ManagedComponent
import li.cil.oc.common.tileentity
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import li.cil.oc.util.ExtendedArguments._
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.InventoryUtils
import li.cil.oc.{OpenComputers, Settings, api}
import net.minecraft.block.{Block, BlockFluid}
import net.minecraft.entity.item.{EntityItem, EntityMinecart}
import net.minecraft.entity.{Entity, EntityLivingBase}
import net.minecraft.item.{ItemBlock, ItemStack}
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.{EnumMovingObjectType, MovingObjectPosition}
import net.minecraftforge.common.{ForgeDirection, MinecraftForge}
import net.minecraftforge.event.world.BlockEvent
import net.minecraftforge.fluids._

import scala.collection.convert.WrapAsScala._

class Robot(val robot: tileentity.Robot) extends ManagedComponent {
  val node = api.Network.newNode(this, Visibility.Network).
    withComponent("robot").
    withConnector(Settings.get.bufferRobot).
    create()

  def actualSlot(n: Int) = robot.actualSlot(n)

  def world = robot.world

  def x = robot.x

  def y = robot.y

  def z = robot.z

  // ----------------------------------------------------------------------- //

  val romRobot = Option(api.FileSystem.asManagedEnvironment(api.FileSystem.
    fromClass(OpenComputers.getClass, Settings.resourceDomain, "lua/component/robot"), "robot"))

  def selectedSlot = robot.selectedSlot

  def selectedTank = robot.selectedTank

  def player = robot.player()

  def canPlaceInAir = {
    val event = new RobotPlaceInAirEvent(robot)
    MinecraftForge.EVENT_BUS.post(event)
    event.isAllowed
  }

  @Callback
  def name(context: Context, args: Arguments): Array[AnyRef] = result(robot.name)

  // ----------------------------------------------------------------------- //

  @Callback
  def inventorySize(context: Context, args: Arguments): Array[AnyRef] = result(robot.inventorySize)

  @Callback
  def select(context: Context, args: Arguments): Array[AnyRef] = {
    if (args.count > 0 && args.checkAny(0) != null) {
      val slot = checkSlot(args, 0)
      if (slot != selectedSlot) {
        robot.selectedSlot = slot
        ServerPacketSender.sendRobotSelectedSlotChange(robot)
      }
    }
    result(selectedSlot - actualSlot(0) + 1)
  }

  @Callback(direct = true)
  def count(context: Context, args: Arguments): Array[AnyRef] = {
    val slot =
      if (args.count > 0 && args.checkAny(0) != null) checkSlot(args, 0)
      else selectedSlot
    result(stackInSlot(slot) match {
      case Some(stack) => stack.stackSize
      case _ => 0
    })
  }

  @Callback(direct = true)
  def space(context: Context, args: Arguments): Array[AnyRef] = {
    val slot =
      if (args.count > 0 && args.checkAny(0) != null) checkSlot(args, 0)
      else selectedSlot
    result(stackInSlot(slot) match {
      case Some(stack) => math.min(robot.getInventoryStackLimit, stack.getMaxStackSize) - stack.stackSize
      case _ => robot.getInventoryStackLimit
    })
  }

  @Callback
  def compareTo(context: Context, args: Arguments): Array[AnyRef] = {
    val slot = checkSlot(args, 0)
    result((stackInSlot(selectedSlot), stackInSlot(slot)) match {
      case (Some(stackA), Some(stackB)) => haveSameItemType(stackA, stackB)
      case (None, None) => true
      case _ => false
    })
  }

  @Callback
  def transferTo(context: Context, args: Arguments): Array[AnyRef] = {
    val slot = checkSlot(args, 0)
    val count = args.optionalItemCount(1)
    if (slot == selectedSlot || count == 0) {
      result(true)
    }
    else result((stackInSlot(selectedSlot), stackInSlot(slot)) match {
      case (Some(from), Some(to)) =>
        if (haveSameItemType(from, to)) {
          val space = math.min(robot.getInventoryStackLimit, to.getMaxStackSize) - to.stackSize
          val amount = math.min(count, math.min(space, from.stackSize))
          if (amount > 0) {
            from.stackSize -= amount
            to.stackSize += amount
            assert(from.stackSize >= 0)
            if (from.stackSize == 0) {
              robot.setInventorySlotContents(selectedSlot, null)
            }
            robot.onInventoryChanged()
            true
          }
          else false
        }
        else if (count >= from.stackSize) {
          robot.setInventorySlotContents(slot, from)
          robot.setInventorySlotContents(selectedSlot, to)
          true
        }
        else false
      case (Some(from), None) =>
        robot.setInventorySlotContents(slot, robot.decrStackSize(selectedSlot, count))
        true
      case _ => false
    })
  }

  @Callback
  def compare(context: Context, args: Arguments): Array[AnyRef] = {
    val side = checkSideForAction(args, 0)
    stackInSlot(selectedSlot) match {
      case Some(stack) => Option(stack.getItem) match {
        case Some(item: ItemBlock) =>
          val (bx, by, bz) = (x + side.offsetX, y + side.offsetY, z + side.offsetZ)
          val idMatches = item.getBlockID == world.getBlockId(bx, by, bz)
          val subTypeMatches = !item.getHasSubtypes || item.getMetadata(stack.getItemDamage) == world.getBlockMetadata(bx, by, bz)
          return result(idMatches && subTypeMatches)
        case _ =>
      }
      case _ =>
    }
    result(false)
  }

  @Callback
  def drop(context: Context, args: Arguments): Array[AnyRef] = {
    val facing = checkSideForAction(args, 0)
    val count = args.optionalItemCount(1)
    val stack = robot.getStackInSlot(selectedSlot)
    if (stack != null && stack.stackSize > 0) {
      val player = robot.player(facing)
      InventoryUtils.inventoryAt(world, x + facing.offsetX, y + facing.offsetY, z + facing.offsetZ) match {
        case Some(inventory) if inventory.isUseableByPlayer(player) =>
          if (!InventoryUtils.insertIntoInventory(stack, inventory, facing.getOpposite, count)) {
            // Cannot drop into that inventory.
            return result(false, "inventory full")
          }
          else if (stack.stackSize == 0) {
            // Dropped whole stack.
            robot.setInventorySlotContents(selectedSlot, null)
          }
          else {
            // Dropped partial stack.
            robot.onInventoryChanged()
          }
        case _ =>
          // No inventory to drop into, drop into the world.
          player.dropPlayerItemWithRandomChoice(robot.decrStackSize(selectedSlot, count), inPlace = false)
      }

      context.pause(Settings.get.dropDelay)

      result(true)
    }
    else result(false)
  }

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
        case Some(hit) if hit.typeOfHit == EnumMovingObjectType.TILE =>
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

  @Callback
  def suck(context: Context, args: Arguments): Array[AnyRef] = {
    val facing = checkSideForAction(args, 0)
    val count = args.optionalItemCount(1)

    if (InventoryUtils.inventoryAt(world, x + facing.offsetX, y + facing.offsetY, z + facing.offsetZ).exists(inventory => {
      inventory.isUseableByPlayer(player) && InventoryUtils.extractFromInventory(player.inventory.addItemStackToInventory, inventory, facing.getOpposite, count)
    })) {
      context.pause(Settings.get.suckDelay)
      result(true)
    }
    else {
      for (entity <- player.entitiesOnSide[EntityItem](facing) if !entity.isDead && entity.delayBeforeCanPickup <= 0) {
        val stack = entity.getEntityItem
        val size = stack.stackSize
        entity.onCollideWithPlayer(player)
        if (stack.stackSize < size || entity.isDead) {
          context.pause(Settings.get.suckDelay)
          return result(true)
        }
      }
      result(false)
    }
  }

  // ----------------------------------------------------------------------- //

  @Callback
  def detect(context: Context, args: Arguments): Array[AnyRef] = {
    val side = checkSideForAction(args, 0)
    val (something, what) = blockContent(robot.player(side), side)
    result(something, what)
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

      val (success, what) = Option(pick(player, Settings.get.swingRange)) match {
        case Some(hit) =>
          hit.typeOfHit match {
            case EnumMovingObjectType.ENTITY =>
              attack(player, hit.entityHit)
            case EnumMovingObjectType.TILE =>
              click(player, hit.blockX, hit.blockY, hit.blockZ, hit.sideHit)
          }
        case _ => // Retry with full block bounds, disregarding swing range.
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
        case Some(hit) if hit.typeOfHit == EnumMovingObjectType.ENTITY && interact(player, hit.entityHit) =>
          triggerDelay()
          (true, "item_interacted")
        case Some(hit) if hit.typeOfHit == EnumMovingObjectType.TILE =>
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
        if (item.isItemStackDamageable) {
          result(item.getMaxDamage - item.getItemDamage, item.getMaxDamage - item.getItemDamageForDisplay, item.getMaxDamage)
        }
        else result(Unit, "tool cannot be damaged")
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
      val (something, what) = blockContent(robot.player(direction), direction)
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
    val index = checkSlot(args, 0)
    val count = args.optionalFluidCount(1)
    if (index == selectedTank || count == 0) {
      result(true)
    }
    else result((getTank(selectedTank), getTank(index)) match {
      case (Some(from), Some(to)) =>
        if (haveSameFluidType(from.getFluid, to.getFluid)) {
          val space = to.getCapacity - to.getFluidAmount
          val amount = math.min(count, space)
          if (amount > 0) {
            to.fill(from.drain(amount, true), true)
            robot.onInventoryChanged()
            true
          }
          else false
        }
        else if (count >= from.getFluidAmount) {
          val tmp = to.drain(to.getFluidAmount, true)
          to.fill(from.drain(from.getFluidAmount, true), true)
          from.fill(tmp, true)
          robot.onInventoryChanged()
          true
        }
        else false
      case _ => false
    })
  }

  @Callback
  def compareFluid(context: Context, args: Arguments): Array[AnyRef] = {
    val side = checkSideForAction(args, 0)
    fluidInTank(selectedTank) match {
      case Some(stack) =>
        world.getBlockTileEntity(x + side.offsetX, y + side.offsetY, z + side.offsetZ) match {
          case handler: IFluidHandler =>
            result(Option(handler.getTankInfo(side.getOpposite)).exists(_.exists(other => stack.isFluidEqual(other.fluid))))
          case _ =>
            val blockId = world.getBlockId(x + side.offsetX, y + side.offsetY, z + side.offsetZ)
            val fluid = FluidRegistry.lookupFluidForBlock(Block.blocksList(blockId))
            result(stack.getFluid == fluid)
        }
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
        else world.getBlockTileEntity(x + facing.offsetX, y + facing.offsetY, z + facing.offsetZ) match {
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
            val blockId = world.getBlockId(x + facing.offsetX, y + facing.offsetY, z + facing.offsetZ)
            val fluid = FluidRegistry.lookupFluidForBlock(Block.blocksList(blockId))
            if (tank.fill(new FluidStack(fluid, 1000), false) == 1000) {
              tank.fill(new FluidStack(fluid, 1000), true)
              world.setBlockToAir(x + facing.offsetX, y + facing.offsetY, z + facing.offsetZ)
              result(true)
            }
            else result(Unit, "tank is full")
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
        world.getBlockTileEntity(bx, by, bz) match {
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
            val blockId = world.getBlockId(bx, by, bz)
            val block = Block.blocksList(blockId)
            if (blockId > 0 && block != null && !block.isAirBlock(world, x, y, z) && !block.isBlockReplaceable(world, x, y, z)) {
              result(Unit, "no space")
            }
            else if (tank.getFluidAmount < 1000) {
              result(Unit, "tank is empty")
            }
            else if (!tank.getFluid.getFluid.canBePlacedInWorld) {
              result(Unit, "incompatible fluid")
            }
            else {
              val fluidBlockId = tank.getFluid.getFluid.getBlockID
              tank.drain(1000, true)
              world.destroyBlock(bx, by, bz, true)
              world.setBlock(bx, by, bz, fluidBlockId)
              // This fake neighbor update is required to get stills to start flowing.
              world.notifyBlockOfNeighborChange(bx, by, bz, 0)
              result(true)
            }
        }
      case _ => result(Unit, "no tank selected")
    }
  }

//  @Callback
//  def getFluidInfo(context: Context, args: Arguments): Array[AnyRef] = {
//    robot.getSelectedTank match {
//      case Some(component) =>
//        component.getInfo match {
//          case (info: FluidTankInfo) => info.fluid match {
//            case (fluid: FluidStack) => result(fluid.getFluid.getName, fluid.getFluid.getLocalizedName, fluid.amount, info.capacity)
//            case _ => result(Unit, "no fluid")
//          }
//          case _ => result(Unit, "no fluid")
//        }
//      case None => result(Unit, "no Container Selected")
//    }
//  }
//
//  @Callback
//  def storeFluid(context: Context, args: Arguments): Array[AnyRef] = {
//    val stack = stackInSlot(selectedSlot)
//    stack match {
//      case Some(item) => return result(FluidContainerRegistry.isBucket(item), FluidContainerRegistry.isContainer(item))
//      case None =>
//    }
//    result(Unit, "Not a valid inventory")
//  }
//
//  @Callback
//  def ejectFluid(context: Context, args: Arguments): Array[AnyRef] = {
//    val stack = stackInSlot(selectedSlot)
//    stack match {
//      case Some(item) => result(true)
//      case None => result(Unit, "Not a valid inventory")
//    }
//  }

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

  private def blockContent(player: Player, side: ForgeDirection) = {
    player.closestEntity[Entity](side) match {
      case Some(_@(_: EntityLivingBase | _: EntityMinecart)) =>
        (true, "entity")
      case _ =>
        val (bx, by, bz) = (x + side.offsetX, y + side.offsetY, z + side.offsetZ)
        val id = world.getBlockId(bx, by, bz)
        val block = Block.blocksList(id)
        val metadata = world.getBlockMetadata(bx, by, bz)
        if (id == 0 || block == null || block.isAirBlock(world, bx, by, bz)) {
          (false, "air")
        }
        else if (FluidRegistry.lookupFluidForBlock(block) != null || block.isInstanceOf[BlockFluid]) {
          val event = new BlockEvent.BreakEvent(bx, by, bz, world, block, metadata, player)
          MinecraftForge.EVENT_BUS.post(event)
          (event.isCanceled, "liquid")
        }
        else if (block.isBlockReplaceable(world, bx, by, bz)) {
          val event = new BlockEvent.BreakEvent(bx, by, bz, world, block, metadata, player)
          MinecraftForge.EVENT_BUS.post(event)
          (event.isCanceled, "replaceable")
        }
        else {
          (true, "solid")
        }
    }
  }

  private def pick(player: Player, range: Double) = {
    val origin = world.getWorldVec3Pool.getVecFromPool(
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
    val hit = world.clip(origin, target)
    player.closestEntity[Entity]() match {
      case Some(entity@(_: EntityLivingBase | _: EntityMinecart)) if hit == null || world.getWorldVec3Pool.getVecFromPool(player.posX, player.posY, player.posZ).distanceTo(hit.hitVec) > player.getDistanceToEntity(entity) => new MovingObjectPosition(entity)
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

  private def haveSameItemType(stackA: ItemStack, stackB: ItemStack) =
    stackA.itemID == stackB.itemID &&
      (!stackA.getHasSubtypes || stackA.getItemDamage == stackB.getItemDamage)

  private def haveSameFluidType(stackA: FluidStack, stackB: FluidStack) = stackA.isFluidEqual(stackB)

  private def stackInSlot(slot: Int) = Option(robot.getStackInSlot(slot))

  private def getTank(tank: Int) = robot.getFluidTank(tank)

  private def fluidInTank(tank: Int) = getTank(tank).map(_.getFluid)

  // ----------------------------------------------------------------------- //

  private def checkSlot(args: Arguments, n: Int) = args.checkSlot(robot, n)

  private def checkTank(args: Arguments, n: Int) = args.checkTank(robot, n)

  private def checkSideForAction(args: Arguments, n: Int) = robot.toGlobal(args.checkSideForAction(n))

  private def checkSideForFace(args: Arguments, n: Int, facing: ForgeDirection) = robot.toGlobal(args.checkSideForFace(n, robot.toLocal(facing)))
}
