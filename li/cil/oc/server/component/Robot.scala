package li.cil.oc.server.component

import li.cil.oc.Config
import li.cil.oc.api.network.{LuaCallback, Arguments, Context}
import li.cil.oc.common.tileentity
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import li.cil.oc.util.ActivationType
import net.minecraft.block.{BlockFluid, Block}
import net.minecraft.entity.item.EntityItem
import net.minecraft.item.{ItemStack, ItemBlock}
import net.minecraft.util.{MovingObjectPosition, EnumMovingObjectType, Vec3}
import net.minecraftforge.common.ForgeDirection
import net.minecraftforge.fluids.FluidRegistry
import scala.Some

class Robot(val robot: tileentity.Robot) extends Computer(robot) {

  def selectedSlot = robot.selectedSlot

  def actualSlot(n: Int) = robot.actualSlot(n)

  def world = robot.world

  def x = robot.x

  def y = robot.y

  def z = robot.z

  // ----------------------------------------------------------------------- //

  override def isRobot = true

  // ----------------------------------------------------------------------- //

  @LuaCallback("select")
  def select(context: Context, args: Arguments): Array[AnyRef] = {
    if (args.count > 0 && args.checkAny(0) != null) {
      val slot = checkSlot(args, 0)
      if (slot != selectedSlot) {
        robot.selectedSlot = slot
        ServerPacketSender.sendRobotSelectedSlotState(robot)
      }
    }
    result(selectedSlot + 1)
  }

  @LuaCallback("count")
  def count(context: Context, args: Arguments): Array[AnyRef] =
    result(stackInSlot(selectedSlot) match {
      case Some(stack) => stack.stackSize
      case _ => 0
    })

  @LuaCallback("space")
  def space(context: Context, args: Arguments): Array[AnyRef] =
    result(stackInSlot(selectedSlot) match {
      case Some(stack) => robot.getInventoryStackLimit - stack.stackSize
      case _ => robot.getInventoryStackLimit
    })

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
          val space = (robot.getInventoryStackLimit min to.getMaxStackSize) - to.stackSize
          val amount = count min space min from.stackSize
          if (amount > 0) {
            from.stackSize -= amount
            to.stackSize += amount
            assert(from.stackSize >= 0)
            if (from.stackSize == 0) {
              robot.setInventorySlotContents(actualSlot(selectedSlot), null)
            }
            true
          }
          else false
        }
        else {
          robot.setInventorySlotContents(actualSlot(slot), from)
          robot.setInventorySlotContents(actualSlot(selectedSlot), to)
          true
        }
      case (Some(from), None) =>
        robot.setInventorySlotContents(actualSlot(slot), robot.decrStackSize(actualSlot(selectedSlot), count))
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
    // TODO inventory, if available

    result(robot.dropSlot(actualSlot(selectedSlot), count, facing))
    // This also works, but throws the items a little too far.
    // result(robot.player().dropPlayerItemWithRandomChoice(robot.decrStackSize(actualSlot(selectedSlot), count), false) != null)
  }

  @LuaCallback("place")
  def place(context: Context, args: Arguments): Array[AnyRef] = {
    val facing = checkSideForAction(args, 0)
    val side = if (args.isInteger(1)) checkSide(args, 1) else facing
    if (side.getOpposite == facing) {
      throw new IllegalArgumentException("invalid side")
    }
    val sneaky = args.isBoolean(2) && args.checkBoolean(2)
    val player = robot.player(facing, side)
    val stack = player.robotInventory.selectedItemStack
    if (stack == null || stack.stackSize == 0) {
      result(false, "nothing selected")
    }
    else {
      player.setSneaking(sneaky)
      val what = Option(pick(facing, side, 0.51)) match {
        case Some(hit) if hit.typeOfHit == EnumMovingObjectType.TILE =>
          val (bx, by, bz, hx, hy, hz) = clickParamsFromHit(hit)
          player.placeBlock(stack, bx, by, bz, hit.sideHit, hx, hy, hz)
        case None if Config.canPlaceInAir && player.closestLivingEntity(facing).isEmpty =>
          val (bx, by, bz, hx, hy, hz) = clickParamsFromFacing(facing, side)
          player.placeBlock(stack, bx, by, bz, side.getOpposite.ordinal, hx, hy, hz)
        case _ => false
      }
      player.setSneaking(false)
      if (stack.stackSize <= 0) {
        robot.setInventorySlotContents(player.robotInventory.selectedSlot, null)
      }
      result(what)
    }
  }

  @LuaCallback("suck")
  def suck(context: Context, args: Arguments): Array[AnyRef] = {
    val facing = checkSideForAction(args, 0)
    val count = checkOptionalItemCount(args, 1)
    // TODO inventory, if available
    for (entity <- robot.player().entitiesOnSide[EntityItem](facing) if !entity.isDead && entity.delayBeforeCanPickup <= 0) {
      val stack = entity.getEntityItem
      val size = stack.stackSize
      entity.onCollideWithPlayer(robot.player())
      if (stack.stackSize < size || entity.isDead) return result(true)
    }
    result(false)
  }

  // ----------------------------------------------------------------------- //

  @LuaCallback("detect")
  def detect(context: Context, args: Arguments): Array[AnyRef] = {
    val side = checkSideForAction(args, 0)
    val (bx, by, bz) = (x + side.offsetX, y + side.offsetY, z + side.offsetZ)
    val id = world.getBlockId(bx, by, bz)
    val block = Block.blocksList(id)
    if (id == 0 || block == null || block.isAirBlock(world, bx, by, bz)) {
      robot.player().closestLivingEntity(side) match {
        case Some(entity) => result(true, "entity")
        case _ => result(false, "air")
      }
    }
    else if (FluidRegistry.lookupFluidForBlock(block) != null || block.isInstanceOf[BlockFluid]) {
      result(false, "liquid")
    }
    else if (block.isBlockReplaceable(world, bx, by, bz)) {
      result(false, "replaceable")
    }
    else {
      result(true, "solid")
    }
  }

  // ----------------------------------------------------------------------- //

  @LuaCallback("swing")
  def swing(context: Context, args: Arguments): Array[AnyRef] = {
    // Swing the equipped tool (left click).
    val facing = checkSideForAction(args, 0)
    val side = if (args.isInteger(1)) checkSide(args, 1) else facing
    if (side.getOpposite == facing) {
      throw new IllegalArgumentException("invalid side")
    }
    val player = robot.player(facing, side)
    Option(pick(facing, side, 0.49)) match {
      case Some(hit) =>
        val what = hit.typeOfHit match {
          case EnumMovingObjectType.ENTITY =>
            player.attackTargetEntityWithCurrentItem(hit.entityHit)
            result(true, "entity")
          case EnumMovingObjectType.TILE =>
            val broke = player.clickBlock(hit.blockX, hit.blockY, hit.blockZ, hit.sideHit)
            result(broke, "block")
        }
        what
      case _ =>
        player.closestLivingEntity(facing) match {
          case Some(entity) =>
            player.attackTargetEntityWithCurrentItem(entity)
            result(true, "entity")
          case _ =>
            result(false)
        }
    }
  }

  @LuaCallback("use")
  def use(context: Context, args: Arguments): Array[AnyRef] = {
    val facing = checkSideForAction(args, 0)
    val side = if (args.isInteger(1)) checkSide(args, 1) else facing
    if (side.getOpposite == facing) {
      throw new IllegalArgumentException("invalid side")
    }
    val sneaky = args.isBoolean(2) && args.checkBoolean(2)
    val player = robot.player(facing, side)
    def activationResult(activationType: ActivationType.Value): Array[AnyRef] =
      activationType match {
        case ActivationType.BlockActivated => result(true, "block_activated")
        case ActivationType.ItemPlaced => result(true, "item_placed")
        case ActivationType.ItemUsed => result(true, "item_used")
        case _ => result(false)
      }
    player.setSneaking(sneaky)
    val what = Option(pick(facing, side, 0.51)) match {
      case Some(hit) =>
        hit.typeOfHit match {
          case EnumMovingObjectType.ENTITY =>
            // TODO Is there any practical use for this? Most of the stuff related to this is still 'obfuscated'...
            result(false, "entity")
          case EnumMovingObjectType.TILE =>
            val (bx, by, bz, hx, hy, hz) = clickParamsFromHit(hit)
            activationResult(player.activateBlockOrUseItem(bx, by, bz, hit.sideHit, hx, hy, hz))
        }
      case _ =>
        (if (Config.canPlaceInAir) {
          val (bx, by, bz, hx, hy, hz) = clickParamsFromFacing(facing, side)
          player.activateBlockOrUseItem(bx, by, bz, side.getOpposite.ordinal, hx, hy, hz)
        } else ActivationType.None) match {
          case ActivationType.None =>
            if (player.useEquippedItem()) result(true, "item_used")
            else result(false)
          case activationType => activationResult(activationType)
        }
    }
    player.setSneaking(false)
    what
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

  def pick(facing: ForgeDirection, side: ForgeDirection, range: Double) = {
    val (bx, by, bz) = (x + facing.offsetX, y + facing.offsetY, z + facing.offsetZ)
    val (hx, hy, hz) = (0.5 + side.offsetX * range, 0.5 + side.offsetY * range, 0.5 + side.offsetZ * range)
    val origin = Vec3.createVectorHelper(x + 0.5, y + 0.5, z + 0.5)
    val target = Vec3.createVectorHelper(bx + hx, by + hy, bz + hz)
    world.clip(origin, target)
  }

  // ----------------------------------------------------------------------- //

  @LuaCallback("move")
  def move(context: Context, args: Arguments): Array[AnyRef] = {
    // Try to move in the specified direction.
    val side = checkSideForMovement(args, 0)
    null
  }

  @LuaCallback("turn")
  def turn(context: Context, args: Arguments): Array[AnyRef] = {
    val clockwise = args.checkBoolean(0)
    if (clockwise) robot.rotate(ForgeDirection.UP)
    else robot.rotate(ForgeDirection.DOWN)
    result(true)
  }

  // ----------------------------------------------------------------------- //

  private def haveSameItemType(stackA: ItemStack, stackB: ItemStack) =
    stackA.itemID == stackB.itemID &&
      (!stackA.getHasSubtypes || stackA.getItemDamage == stackB.getItemDamage)

  private def stackInSlot(slot: Int) = Option(robot.getStackInSlot(actualSlot(slot)))

  private def clickParamsFromHit(hit: MovingObjectPosition) = {
    (hit.blockX, hit.blockY, hit.blockZ,
      (hit.hitVec.xCoord - hit.blockX).toFloat,
      (hit.hitVec.yCoord - hit.blockY).toFloat,
      (hit.hitVec.zCoord - hit.blockZ).toFloat)
  }

  private def clickParamsFromFacing(facing: ForgeDirection, side: ForgeDirection) = {
    (x + facing.offsetX + side.offsetX,
      y + facing.offsetY + side.offsetY,
      z + facing.offsetZ + side.offsetZ,
      0.5f - side.offsetX * 0.5f,
      0.5f - side.offsetY * 0.5f,
      0.5f - side.offsetZ * 0.5f)
  }

  // ----------------------------------------------------------------------- //

  private def checkOptionalItemCount(args: Arguments, n: Int) =
    if (args.count > n && args.checkAny(n) != null) {
      args.checkInteger(n) max 0 min robot.getInventoryStackLimit
    }
    else robot.getInventoryStackLimit

  private def checkSlot(args: Arguments, n: Int) = {
    val slot = args.checkInteger(n) - 1
    if (slot < 0 || slot > 15) {
      throw new IllegalArgumentException("invalid slot")
    }
    slot
  }

  private def checkSideForAction(args: Arguments, n: Int) = checkSide(args, n, ForgeDirection.SOUTH, ForgeDirection.UP, ForgeDirection.DOWN)

  private def checkSideForMovement(args: Arguments, n: Int) = checkSide(args, n, ForgeDirection.SOUTH, ForgeDirection.NORTH)

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
