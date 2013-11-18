package li.cil.oc.server.component

import li.cil.oc.api.network.{LuaCallback, Arguments, Context}
import li.cil.oc.common.tileentity
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import net.minecraft.block.Block
import net.minecraft.entity.item.EntityItem
import net.minecraft.entity.{Entity, EntityLivingBase}
import net.minecraft.item.{ItemStack, ItemBlock}
import net.minecraft.util.AxisAlignedBB
import net.minecraftforge.common.ForgeDirection
import net.minecraftforge.fluids.FluidRegistry
import scala.Some
import scala.collection.convert.WrapAsScala._
import scala.reflect._

class Robot(val robot: tileentity.Robot) extends Computer(robot) {

  override def isRobot(context: Context, args: Arguments): Array[AnyRef] =
    Array(java.lang.Boolean.TRUE)

  // ----------------------------------------------------------------------- //

  @LuaCallback("select")
  def select(context: Context, args: Arguments): Array[AnyRef] = {
    if (args.count > 0 && args.checkAny(0) != null) {
      val slot = checkSlot(args, 0)
      if (slot != robot.selectedSlot) {
        robot.selectedSlot = slot
        ServerPacketSender.sendRobotSelectedSlotState(robot)
      }
    }
    result(robot.selectedSlot)
  }

  @LuaCallback("count")
  def count(context: Context, args: Arguments): Array[AnyRef] =
    result(stackInSlot(robot.selectedSlot) match {
      case Some(stack) => stack.stackSize
      case _ => 0
    })

  @LuaCallback("space")
  def space(context: Context, args: Arguments): Array[AnyRef] =
    result(stackInSlot(robot.selectedSlot) match {
      case Some(stack) => robot.getInventoryStackLimit - stack.stackSize
      case _ => robot.getInventoryStackLimit
    })

  @LuaCallback("compareTo")
  def compareTo(context: Context, args: Arguments): Array[AnyRef] = {
    val slot = checkSlot(args, 0)
    result((stackInSlot(robot.selectedSlot), stackInSlot(slot)) match {
      case (Some(stackA), Some(stackB)) => haveSameItemType(stackA, stackB)
      case (None, None) => true
      case _ => false
    })
  }

  @LuaCallback("transferTo")
  def transferTo(context: Context, args: Arguments): Array[AnyRef] = {
    val slot = checkSlot(args, 0)
    val count = checkOptionalItemCount(args, 1)
    if (slot == robot.selectedSlot || count == 0) {
      result(true)
    }
    else result((stackInSlot(robot.selectedSlot), stackInSlot(slot)) match {
      case (Some(from), Some(to)) =>
        if (haveSameItemType(from, to)) {
          val space = (robot.getInventoryStackLimit min to.getMaxStackSize) - to.stackSize
          val amount = count min space min from.stackSize
          if (amount > 0) {
            from.stackSize -= amount
            to.stackSize += amount
            assert(from.stackSize >= 0)
            if (from.stackSize == 0) {
              robot.setInventorySlotContents(robot.actualSlot(robot.selectedSlot), null)
            }
            true
          }
          else false
        }
        else {
          robot.setInventorySlotContents(robot.actualSlot(slot), from)
          robot.setInventorySlotContents(robot.actualSlot(robot.selectedSlot), to)
          true
        }
      case (Some(from), None) =>
        robot.setInventorySlotContents(robot.actualSlot(slot), robot.decrStackSize(robot.actualSlot(robot.selectedSlot), count))
        true
      case _ => false
    })
  }

  @LuaCallback("drop")
  def drop(context: Context, args: Arguments): Array[AnyRef] = {
    val side = checkSideForAction(args, 0)
    val count = checkOptionalItemCount(args, 1)
    // TODO inventory, if available
    result(robot.dropSlot(robot.actualSlot(robot.selectedSlot), count, side))
  }

  @LuaCallback("place")
  def place(context: Context, args: Arguments): Array[AnyRef] = {
    // Place block item selected in inventory.
    val side = checkSideForAction(args, 0)
    null
  }

  @LuaCallback("suck")
  def suck(context: Context, args: Arguments): Array[AnyRef] = {
    // Pick up items lying around.
    val side = checkSideForAction(args, 0)
    val count = checkOptionalItemCount(args, 1)
    // TODO inventory, if available
    for (entity <- entitiesOnSide[EntityItem](side) if !entity.isDead && entity.delayBeforeCanPickup <= 0) {
      val stack = entity.getEntityItem
      val size = stack.stackSize
      entity.onCollideWithPlayer(robot.player)
      if (stack.stackSize < size || entity.isDead) return result(true)
    }
    result(false)
  }

  // ----------------------------------------------------------------------- //

  @LuaCallback("compare")
  def compare(context: Context, args: Arguments): Array[AnyRef] = {
    val side = checkSideForAction(args, 0)
    stackInSlot(robot.selectedSlot) match {
      case Some(stack) => Option(stack.getItem) match {
        case Some(item: ItemBlock) =>
          val (bx, by, bz) = (robot.x + side.offsetX, robot.y + side.offsetY, robot.z + side.offsetZ)
          val idMatches = item.getBlockID == robot.world.getBlockId(bx, by, bz)
          val subTypeMatches = !item.getHasSubtypes || item.getMetadata(stack.getItemDamage) == robot.world.getBlockMetadata(bx, by, bz)
          return result(idMatches && subTypeMatches)
        case _ =>
      }
      case _ =>
    }
    result(false)
  }

  @LuaCallback("detect")
  def detect(context: Context, args: Arguments): Array[AnyRef] = {
    val side = checkSideForAction(args, 0)
    val (bx, by, bz) = (robot.x + side.offsetX, robot.y + side.offsetY, robot.z + side.offsetZ)
    val id = robot.world.getBlockId(bx, by, bz)
    val block = Block.blocksList(id)
    if (id == 0 || block == null || block.isAirBlock(robot.world, bx, by, bz)) {
      closestLivingEntity(side) match {
        case Some(entity) => result(true, "entity")
        case _ => result(false, "air")
      }
    }
    else if (FluidRegistry.lookupFluidForBlock(block) != null) {
      result(false, "liquid")
    }
    else if (block.isBlockReplaceable(robot.world, bx, by, bz)) {
      result(false, "replaceable")
    }
    else {
      result(true, "solid")
    }
  }

  // ----------------------------------------------------------------------- //

  @LuaCallback("attack")
  def attack(context: Context, args: Arguments): Array[AnyRef] = {
    // Attack with equipped tool.
    val side = checkSideForAction(args, 0)
    null
  }

  @LuaCallback("use")
  def use(context: Context, args: Arguments): Array[AnyRef] = {
    // Use equipped tool (e.g. dig, chop, till).
    val side = checkSideForAction(args, 0)
    val sneaky = args.checkBoolean(1)
    null
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
    // Turn in the specified direction.
    val clockwise = args.checkBoolean(0)
    if (clockwise) robot.rotate(ForgeDirection.UP)
    else robot.rotate(ForgeDirection.DOWN)
    result(true)
  }

  // ----------------------------------------------------------------------- //

  private def closestLivingEntity(side: ForgeDirection) = {
    entitiesOnSide[EntityLivingBase](side).
      foldLeft((Double.PositiveInfinity, None: Option[EntityLivingBase])) {
      case ((bestDistance, bestEntity), entity: EntityLivingBase) =>
        val distance = entity.getDistanceSq(robot.x + 0.5, robot.y + 0.5, robot.z + 0.5)
        if (distance < bestDistance) (distance, Some(entity))
        else (bestDistance, bestEntity)
      case (best, _) => best
    } match {
      case (_, Some(entity)) => Some(entity)
      case _ => None
    }
  }

  private def entitiesOnSide[Type <: Entity : ClassTag](side: ForgeDirection) = {
    val (bx, by, bz) = (robot.x + side.offsetX, robot.y + side.offsetY, robot.z + side.offsetZ)
    val id = robot.world.getBlockId(bx, by, bz)
    val block = Block.blocksList(id)
    if (id == 0 || block == null || block.isAirBlock(robot.world, bx, by, bz)) {
      val bounds = AxisAlignedBB.getAABBPool.getAABB(bx, by, bz, bx + 1, by + 1, bz + 1)
      robot.world.getEntitiesWithinAABB(classTag[Type].runtimeClass, bounds).map(_.asInstanceOf[Type])
    }
    else Iterable.empty
  }

  private def haveSameItemType(stackA: ItemStack, stackB: ItemStack) =
    stackA.itemID == stackB.itemID &&
      (!stackA.getHasSubtypes || stackA.getItemDamage == stackB.getItemDamage)

  private def stackInSlot(slot: Int) = Option(robot.getStackInSlot(robot.actualSlot(slot)))

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
    if (allowed contains direction) robot.toGlobal(direction)
    else throw new IllegalArgumentException("unsupported side")
  }
}
