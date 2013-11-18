package li.cil.oc.server.component

import li.cil.oc.api.network.{LuaCallback, Arguments, Context}
import li.cil.oc.common.tileentity
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import net.minecraft.block.Block
import net.minecraft.entity.item.EntityItem
import net.minecraft.item.{ItemStack, ItemBlock}
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

  override def isRobot(context: Context, args: Arguments): Array[AnyRef] =
    Array(java.lang.Boolean.TRUE)

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
    result(selectedSlot)
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

  @LuaCallback("drop")
  def drop(context: Context, args: Arguments): Array[AnyRef] = {
    val side = checkSideForAction(args, 0)
    val count = checkOptionalItemCount(args, 1)
    // TODO inventory, if available

    result(robot.dropSlot(actualSlot(selectedSlot), count, side))
    // This also works, but throws the items a little too far.
    // result(robot.player().dropPlayerItemWithRandomChoice(robot.decrStackSize(actualSlot(selectedSlot), count), false) != null)
  }

  @LuaCallback("place")
  def place(context: Context, args: Arguments): Array[AnyRef] = {
    val lookSide = checkSideForAction(args, 0)
    val side = if (args.isInteger(1)) checkSide(args, 1) else lookSide
    if (side.getOpposite == lookSide) {
      throw new IllegalArgumentException("invalid side")
    }
    val sneaky = args.isBoolean(2) && args.checkBoolean(2)
    val player = robot.player(lookSide)
    val stack = player.robotInventory.selectedItemStack
    if (stack == null || stack.stackSize == 0) {
      result(false)
    }
    else {
      player.setSneaking(sneaky)
      val (bx, by, bz) = (x + lookSide.offsetX, y + lookSide.offsetY, z + lookSide.offsetZ)
      val (hx, hy, hz) = (0.5f + side.offsetX * 0.5f, 0.5f + side.offsetY * 0.5f, 0.5f + side.offsetZ * 0.5f)
      val ok = player.placeBlock(player.robotInventory.selectedItemStack, bx, by, bz, side.getOpposite.ordinal, hx, hy, hz)
      player.setSneaking(false)
      if (stack.stackSize <= 0) {
        robot.setInventorySlotContents(player.robotInventory.selectedSlot, null)
      }
      result(ok)
    }
  }

  @LuaCallback("suck")
  def suck(context: Context, args: Arguments): Array[AnyRef] = {
    val side = checkSideForAction(args, 0)
    val count = checkOptionalItemCount(args, 1)
    // TODO inventory, if available
    for (entity <- robot.player().entitiesOnSide[EntityItem](side) if !entity.isDead && entity.delayBeforeCanPickup <= 0) {
      val stack = entity.getEntityItem
      val size = stack.stackSize
      entity.onCollideWithPlayer(robot.player())
      if (stack.stackSize < size || entity.isDead) return result(true)
    }
    result(false)
  }

  // ----------------------------------------------------------------------- //

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
    else if (FluidRegistry.lookupFluidForBlock(block) != null) {
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
  def attack(context: Context, args: Arguments): Array[AnyRef] = {
    // Swing the equipped tool (left click).
    val lookSide = checkSideForAction(args, 0)
    val side = if (args.isInteger(1)) checkSide(args, 1) else lookSide
    if (side.getOpposite == lookSide) {
      throw new IllegalArgumentException("invalid side")
    }
    val player = robot.player(lookSide)
    val (bx, by, bz) = (x + lookSide.offsetX, y + lookSide.offsetY, z + lookSide.offsetZ)
    result(player.clickBlock(bx, by, bz, side.getOpposite.ordinal))
    //robot.player().attackTargetEntityWithCurrentItem(entity)
  }

  @LuaCallback("use")
  def use(context: Context, args: Arguments): Array[AnyRef] = {
    val lookSide = checkSideForAction(args, 0)
    val side = if (args.isInteger(1)) checkSide(args, 1) else lookSide
    if (side.getOpposite == lookSide) {
      throw new IllegalArgumentException("invalid side")
    }
    val sneaky = args.isBoolean(2) && args.checkBoolean(2)
    val player = robot.player(lookSide)
    player.setSneaking(sneaky)
    val (bx, by, bz) = (x + lookSide.offsetX, y + lookSide.offsetY, z + lookSide.offsetZ)
    val (hx, hy, hz) = (0.5f + side.offsetX * 0.5f, 0.5f + side.offsetY * 0.5f, 0.5f + side.offsetZ * 0.5f)
    val ok = player.activateBlockOrUseItem(bx, by, bz, side.getOpposite.ordinal, hx, hy, hz)
    player.setSneaking(false)
    result(ok)
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
