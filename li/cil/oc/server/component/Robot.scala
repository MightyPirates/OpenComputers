package li.cil.oc.server.component

import li.cil.oc.Settings
import li.cil.oc.api.network.{RobotContext, LuaCallback, Arguments, Context}
import li.cil.oc.common.tileentity
import li.cil.oc.server.component.robot.{Player, ActivationType}
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import net.minecraft.block.{BlockFluid, Block}
import net.minecraft.entity.item.{EntityMinecart, EntityMinecartContainer, EntityItem}
import net.minecraft.entity.{EntityLivingBase, Entity}
import net.minecraft.inventory.{IInventory, ISidedInventory}
import net.minecraft.item.{ItemStack, ItemBlock}
import net.minecraft.tileentity.TileEntityChest
import net.minecraft.util.{MovingObjectPosition, EnumMovingObjectType}
import net.minecraftforge.common.ForgeDirection
import net.minecraftforge.fluids.FluidRegistry
import scala.Some
import scala.collection.convert.WrapAsScala._

class Robot(val robot: tileentity.Robot) extends Computer(robot) with RobotContext {
  def actualSlot(n: Int) = robot.actualSlot(n)

  def world = robot.world

  def x = robot.x

  def y = robot.y

  def z = robot.z

  // ----------------------------------------------------------------------- //

  override def isRobot = true

  def selectedSlot = robot.selectedSlot

  def player = robot.player()

  def saveUpgrade() = robot.saveUpgrade()

  @LuaCallback(value = "level", direct = true)
  def level(context: Context, args: Arguments): Array[AnyRef] = {
    val xpNeeded = robot.xpForNextLevel - robot.xpForLevel(robot.level)
    val xpProgress = math.max(0, robot.xp - robot.xpForLevel(robot.level))
    result(robot.level + xpProgress / xpNeeded)
  }

  // ----------------------------------------------------------------------- //

  @LuaCallback("select")
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

  @LuaCallback(value = "count", direct = true)
  def count(context: Context, args: Arguments): Array[AnyRef] = {
    val slot =
      if (args.count > 0 && args.checkAny(0) != null) checkSlot(args, 0)
      else selectedSlot
    result(stackInSlot(slot) match {
      case Some(stack) => stack.stackSize
      case _ => 0
    })
  }

  @LuaCallback(value = "space", direct = true)
  def space(context: Context, args: Arguments): Array[AnyRef] = {
    val slot =
      if (args.count > 0 && args.checkAny(0) != null) checkSlot(args, 0)
      else selectedSlot
    result(stackInSlot(slot) match {
      case Some(stack) => math.min(robot.getInventoryStackLimit, stack.getMaxStackSize) - stack.stackSize
      case _ => robot.getInventoryStackLimit
    })
  }

  @LuaCallback("compareTo")
  def compareTo(context: Context, args: Arguments): Array[AnyRef] = {
    val slot = checkSlot(args, 0)
    result((stackInSlot(selectedSlot), stackInSlot(slot)) match {
      case (Some(stackA), Some(stackB)) => haveSameItemType(stackA, stackB)
      case (None, None) => true
      case _ => false
    })
  }

  @LuaCallback("transferTo")
  def transferTo(context: Context, args: Arguments): Array[AnyRef] = {
    val slot = checkSlot(args, 0)
    val count = checkOptionalItemCount(args, 1)
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

  @LuaCallback("compare")
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

  @LuaCallback("drop")
  def drop(context: Context, args: Arguments): Array[AnyRef] = {
    val facing = checkSideForAction(args, 0)
    val count = checkOptionalItemCount(args, 1)
    val dropped = robot.decrStackSize(selectedSlot, count)
    if (dropped != null && dropped.stackSize > 0) {
      def tryDropIntoInventory(inventory: IInventory, filter: (Int) => Boolean) = {
        var success = false
        val maxStackSize = math.min(inventory.getInventoryStackLimit, dropped.getMaxStackSize)
        val shouldTryMerge = !dropped.isItemStackDamageable && dropped.getMaxStackSize > 1 && inventory.getInventoryStackLimit > 1
        if (shouldTryMerge) {
          for (slot <- 0 until inventory.getSizeInventory if dropped.stackSize > 0 && filter(slot)) {
            val existing = inventory.getStackInSlot(slot)
            val shouldMerge = existing != null && existing.stackSize < maxStackSize &&
              existing.isItemEqual(dropped) && ItemStack.areItemStackTagsEqual(existing, dropped)
            if (shouldMerge) {
              val space = maxStackSize - existing.stackSize
              val amount = math.min(space, dropped.stackSize)
              assert(amount > 0)
              success = true
              existing.stackSize += amount
              dropped.stackSize -= amount
            }
          }
        }

        def canDropIntoSlot(slot: Int) = filter(slot) && inventory.isItemValidForSlot(slot, dropped) && inventory.getStackInSlot(slot) == null
        for (slot <- 0 until inventory.getSizeInventory if dropped.stackSize > 0 && canDropIntoSlot(slot)) {
          val amount = math.min(maxStackSize, dropped.stackSize)
          inventory.setInventorySlotContents(slot, dropped.splitStack(amount))
          success = true
        }
        if (success) {
          inventory.onInventoryChanged()
        }
        player.inventory.addItemStackToInventory(dropped)
        success
      }

      world.getBlockTileEntity(x + facing.offsetX, y + facing.offsetY, z + facing.offsetZ) match {
        case chest: TileEntityChest =>
          val inventory = Block.chest.getInventory(world, chest.xCoord, chest.yCoord, chest.zCoord)
          result(tryDropIntoInventory(inventory,
            slot => inventory.isItemValidForSlot(slot, dropped)))
        case inventory: ISidedInventory =>
          result(tryDropIntoInventory(inventory,
            slot => inventory.isItemValidForSlot(slot, dropped) && inventory.canInsertItem(slot, dropped, facing.getOpposite.ordinal())))
        case inventory: IInventory =>
          result(tryDropIntoInventory(inventory,
            slot => inventory.isItemValidForSlot(slot, dropped)))
        case _ =>
          for (entity <- player.entitiesOnSide[EntityMinecartContainer](facing) if entity.isUseableByPlayer(player)) {
            if (tryDropIntoInventory(entity, slot => entity.isItemValidForSlot(slot, dropped))) {
              return result(true)
            }
          }
          player.dropPlayerItemWithRandomChoice(dropped, inPlace = false)
          context.pause(Settings.get.dropDelay)
          result(true)
      }
    }
    else result(false)
  }

  @LuaCallback("place")
  def place(context: Context, args: Arguments): Array[AnyRef] = {
    val facing = checkSideForAction(args, 0)
    val sides =
      if (args.isInteger(1)) {
        Iterable(checkSideForFace(args, 1, facing))
      }
      else {
        ForgeDirection.VALID_DIRECTIONS.filter(_ != facing.getOpposite).toIterable
      }
    val sneaky = args.isBoolean(2) && args.checkBoolean(2)
    val stack = player.robotInventory.selectedItemStack
    if (stack == null || stack.stackSize == 0) {
      return result(false, "nothing selected")
    }

    for (side <- sides) {
      val player = robot.player(facing, side)
      player.setSneaking(sneaky)
      val success = Option(pick(player, Settings.get.useAndPlaceRange)) match {
        case Some(hit) if hit.typeOfHit == EnumMovingObjectType.TILE =>
          val (bx, by, bz, hx, hy, hz) = clickParamsFromHit(hit)
          player.placeBlock(stack, bx, by, bz, hit.sideHit, hx, hy, hz)
        case None if Settings.get.canPlaceInAir && player.closestEntity[Entity]().isEmpty =>
          val (bx, by, bz, hx, hy, hz) = clickParamsFromFacing(facing, side)
          player.placeBlock(stack, bx, by, bz, side.getOpposite.ordinal, hx, hy, hz)
        case _ => false
      }
      player.setSneaking(false)
      if (stack.stackSize <= 0) {
        robot.setInventorySlotContents(player.robotInventory.selectedSlot, null)
      }
      if (success) {
        context.pause(Settings.get.placeDelay)
        robot.animateSwing(Settings.get.placeDelay)
        return result(true)
      }
    }

    result(false)
  }

  @LuaCallback("suck")
  def suck(context: Context, args: Arguments): Array[AnyRef] = {
    val facing = checkSideForAction(args, 0)
    val count = checkOptionalItemCount(args, 1)

    def trySuckFromInventory(inventory: IInventory, filter: (Int) => Boolean) = {
      var success = false
      for (slot <- 0 until inventory.getSizeInventory if !success && filter(slot)) {
        val stack = inventory.getStackInSlot(slot)
        if (stack != null) {
          val maxStackSize = math.min(robot.getInventoryStackLimit, stack.getMaxStackSize)
          val amount = math.min(maxStackSize, math.min(stack.stackSize, count))
          val sucked = stack.splitStack(amount)
          success = player.inventory.addItemStackToInventory(sucked)
          stack.stackSize += sucked.stackSize
          if (stack.stackSize == 0) {
            inventory.setInventorySlotContents(slot, null)
          }
        }
      }
      if (success) {
        inventory.onInventoryChanged()
      }
      success
    }

    world.getBlockTileEntity(x + facing.offsetX, y + facing.offsetY, z + facing.offsetZ) match {
      case chest: TileEntityChest if chest.isUseableByPlayer(player) =>
        val inventory = Block.chest.getInventory(world, chest.xCoord, chest.yCoord, chest.zCoord)
        result(trySuckFromInventory(inventory, slot => true))
      case inventory: ISidedInventory if inventory.isUseableByPlayer(player) =>
        result(trySuckFromInventory(inventory,
          slot => inventory.canExtractItem(slot, inventory.getStackInSlot(slot), facing.getOpposite.ordinal())))
      case inventory: IInventory if inventory.isUseableByPlayer(player) =>
        result(trySuckFromInventory(inventory, slot => true))
      case _ =>
        for (entity <- player.entitiesOnSide[EntityMinecartContainer](facing) if entity.isUseableByPlayer(player)) {
          if (trySuckFromInventory(entity, slot => true)) {
            return result(true)
          }
        }
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

  @LuaCallback("detect")
  def detect(context: Context, args: Arguments): Array[AnyRef] = {
    val side = checkSideForAction(args, 0)
    val (something, what) = blockContent(side)
    result(something, what)
  }

  // ----------------------------------------------------------------------- //

  @LuaCallback("swing")
  def swing(context: Context, args: Arguments): Array[AnyRef] = {
    // Swing the equipped tool (left click).
    val facing = checkSideForAction(args, 0)
    val sides =
      if (args.isInteger(1)) {
        Iterable(checkSideForFace(args, 1, facing))
      }
      else {
        ForgeDirection.VALID_DIRECTIONS.filter(_ != facing.getOpposite).toIterable
      }
    val sneaky = args.isBoolean(2) && args.checkBoolean(2)

    def triggerDelay(delay: Double = Settings.get.swingDelay) = {
      context.pause(delay)
      robot.animateSwing(Settings.get.swingDelay)
    }
    def attack(entity: Entity) = {
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
      endConsumeDrops(entity)
      triggerDelay()
      (true, "entity")
    }
    def click(x: Int, y: Int, z: Int, side: Int) = {
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

    for (side <- sides) {
      val player = robot.player(facing, side)
      player.setSneaking(sneaky)

      val (success, what) = Option(pick(player, Settings.get.swingRange)) match {
        case Some(hit) =>
          hit.typeOfHit match {
            case EnumMovingObjectType.ENTITY =>
              attack(hit.entityHit)
            case EnumMovingObjectType.TILE =>
              click(hit.blockX, hit.blockY, hit.blockZ, hit.sideHit)
          }
        case _ => // Retry with full block bounds, disregarding swing range.
          player.closestEntity[EntityLivingBase]() match {
            case Some(entity) =>
              attack(entity)
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
    }

    result(false)
  }

  @LuaCallback("use")
  def use(context: Context, args: Arguments): Array[AnyRef] = {
    val facing = checkSideForAction(args, 0)
    val sides =
      if (args.isInteger(1)) {
        Iterable(checkSideForFace(args, 1, facing))
      }
      else {
        ForgeDirection.VALID_DIRECTIONS.filter(_ != facing.getOpposite).toIterable
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

    for (side <- sides) {
      val player = robot.player(facing, side)
      player.setSneaking(sneaky)

      def interact(entity: Entity) = {
        beginConsumeDrops(entity)
        val result = player.interactWith(entity)
        endConsumeDrops(entity)
        result
      }
      val (success, what) = Option(pick(player, Settings.get.useAndPlaceRange)) match {
        case Some(hit) if hit.typeOfHit == EnumMovingObjectType.ENTITY && interact(hit.entityHit) =>
          triggerDelay()
          (true, "item_interacted")
        case Some(hit) if hit.typeOfHit == EnumMovingObjectType.TILE =>
          val (bx, by, bz, hx, hy, hz) = clickParamsFromHit(hit)
          activationResult(player.activateBlockOrUseItem(bx, by, bz, hit.sideHit, hx, hy, hz, duration))
        case _ =>
          (if (Settings.get.canPlaceInAir) {
            val (bx, by, bz, hx, hy, hz) = clickParamsFromFacing(facing, side)
            player.activateBlockOrUseItem(bx, by, bz, side.getOpposite.ordinal, hx, hy, hz, duration)
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

  @LuaCallback("durability")
  def durability(context: Context, args: Arguments): Array[AnyRef] = {
    Option(robot.getStackInSlot(0)) match {
      case Some(item) =>
        if (item.isItemStackDamageable) {
          result(item.getMaxDamage - item.getItemDamage)
        }
        else result(Unit, "tool cannot be damaged")
      case _ => result(Unit, "no tool equipped")
    }
  }

  // ----------------------------------------------------------------------- //

  @LuaCallback("move")
  def move(context: Context, args: Arguments): Array[AnyRef] = {
    val direction = checkSideForMovement(args, 0)
    if (robot.isAnimatingMove) {
      // This shouldn't really happen due to delays being enforced, but just to
      // be on the safe side...
      result(false, "already moving")
    }
    else {
      val (something, what) = blockContent(direction)
      if (something) {
        result(false, what)
      }
      else {
        if (!robot.computer.node.tryChangeBuffer(-Settings.get.robotMoveCost)) {
          result(false, "not enough energy")
        }
        else if (robot.move(direction)) {
          context.pause(Settings.get.moveDelay)
          result(true)
        }
        else {
          robot.computer.node.changeBuffer(Settings.get.robotMoveCost)
          result(false, "impossible move")
        }
      }
    }
  }

  @LuaCallback("turn")
  def turn(context: Context, args: Arguments): Array[AnyRef] = {
    val clockwise = args.checkBoolean(0)
    if (robot.computer.node.tryChangeBuffer(-Settings.get.robotTurnCost)) {
      if (clockwise) robot.rotate(ForgeDirection.UP)
      else robot.rotate(ForgeDirection.DOWN)
      robot.animateTurn(clockwise, Settings.get.turnDelay)
      context.pause(Settings.get.turnDelay)
      result(true)
    }
    else {
      result(false, "not enough energy")
    }
  }

  // ----------------------------------------------------------------------- //

  private def beginConsumeDrops(entity: Entity) {
    entity.captureDrops = true
  }

  private def endConsumeDrops(entity: Entity) {
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

  private def blockContent(side: ForgeDirection) = {
    player.closestEntity[Entity](side) match {
      case Some(_@(_: EntityLivingBase | _: EntityMinecart)) =>
        (true, "entity")
      case _ =>
        val (bx, by, bz) = (x + side.offsetX, y + side.offsetY, z + side.offsetZ)
        val id = world.getBlockId(bx, by, bz)
        val block = Block.blocksList(id)
        if (id == 0 || block == null || block.isAirBlock(world, bx, by, bz)) {
          (false, "air")
        }
        else if (FluidRegistry.lookupFluidForBlock(block) != null || block.isInstanceOf[BlockFluid]) {
          (false, "liquid")
        }
        else if (block.isBlockReplaceable(world, bx, by, bz)) {
          (false, "replaceable")
        }
        else {
          (true, "solid")
        }
    }
  }

  private def clickParamsFromFacing(facing: ForgeDirection, side: ForgeDirection) = {
    (x + facing.offsetX + side.offsetX,
      y + facing.offsetY + side.offsetY,
      z + facing.offsetZ + side.offsetZ,
      0.5f - side.offsetX * 0.5f,
      0.5f - side.offsetY * 0.5f,
      0.5f - side.offsetZ * 0.5f)
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

  private def clickParamsFromHit(hit: MovingObjectPosition) = {
    (hit.blockX, hit.blockY, hit.blockZ,
      (hit.hitVec.xCoord - hit.blockX).toFloat,
      (hit.hitVec.yCoord - hit.blockY).toFloat,
      (hit.hitVec.zCoord - hit.blockZ).toFloat)
  }

  // ----------------------------------------------------------------------- //

  private def haveSameItemType(stackA: ItemStack, stackB: ItemStack) =
    stackA.getItem == stackB.getItem &&
      (!stackA.getHasSubtypes || stackA.getItemDamage == stackB.getItemDamage)

  private def stackInSlot(slot: Int) = Option(robot.getStackInSlot(slot))

  // ----------------------------------------------------------------------- //

  private def checkOptionalItemCount(args: Arguments, n: Int) =
    if (args.count > n && args.checkAny(n) != null) {
      math.max(args.checkInteger(n), math.min(0, robot.getInventoryStackLimit))
    }
    else robot.getInventoryStackLimit

  private def checkSlot(args: Arguments, n: Int) = {
    val slot = args.checkInteger(n) - 1
    if (slot < 0 || slot > 15) {
      throw new IllegalArgumentException("invalid slot")
    }
    actualSlot(slot)
  }

  private def checkSideForAction(args: Arguments, n: Int) = checkSide(args, n, ForgeDirection.SOUTH, ForgeDirection.UP, ForgeDirection.DOWN)

  private def checkSideForMovement(args: Arguments, n: Int) = checkSide(args, n, ForgeDirection.SOUTH, ForgeDirection.NORTH, ForgeDirection.UP, ForgeDirection.DOWN)

  private def checkSideForFace(args: Arguments, n: Int, facing: ForgeDirection) = checkSide(args, n, ForgeDirection.VALID_DIRECTIONS.filter(_ != robot.toLocal(facing).getOpposite): _*)

  private def checkSide(args: Arguments, n: Int, allowed: ForgeDirection*) = {
    val side = args.checkInteger(n)
    if (side < 0 || side > 5) {
      throw new IllegalArgumentException("invalid side")
    }
    val direction = ForgeDirection.getOrientation(side)
    if (allowed.isEmpty || (allowed contains direction)) robot.toGlobal(direction)
    else throw new IllegalArgumentException("unsupported side")
  }
}
