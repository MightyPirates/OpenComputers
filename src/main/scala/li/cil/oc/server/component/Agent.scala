package li.cil.oc.server.component

import li.cil.oc.Settings
import li.cil.oc.api.event.RobotPlaceInAirEvent
import li.cil.oc.api.internal
import li.cil.oc.api.internal.MultiTank
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.common.entity
import li.cil.oc.server.agent.ActivationType
import li.cil.oc.server.agent.Player
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedArguments._
import li.cil.oc.util.ExtendedWorld._
import li.cil.oc.util.InventoryUtils
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityMinecart
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.IInventory
import net.minecraft.util.EnumActionResult
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.RayTraceResult
import net.minecraft.util.math.Vec3d
import net.minecraftforge.common.MinecraftForge

import scala.collection.convert.WrapAsScala._

trait Agent extends traits.WorldControl with traits.InventoryControl with traits.InventoryWorldControl with traits.TankAware with traits.TankControl with traits.TankWorldControl {
  def agent: internal.Agent

  override def position = BlockPosition(agent)

  override def fakePlayer: EntityPlayer = agent.player

  protected def rotatedPlayer(facing: EnumFacing = agent.facing, side: EnumFacing = agent.facing): Player = {
    val player = agent.player.asInstanceOf[Player]
    Player.updatePositionAndRotation(player, facing, side)
    // no need to set inventory, calling agent.Player already did that
    //Player.setInventoryPlayerItems(player)
    player
  }

  // ----------------------------------------------------------------------- //

  override def inventory: IInventory = agent.mainInventory

  override def selectedSlot: Int = agent.selectedSlot

  override def selectedSlot_=(value: Int): Unit = agent.setSelectedSlot(value)

  // ----------------------------------------------------------------------- //

  override def tank: MultiTank = agent.tank

  def selectedTank: Int = agent.selectedTank

  override def selectedTank_=(value: Int): Unit = agent.setSelectedTank(value)

  // ----------------------------------------------------------------------- //

  def canPlaceInAir: Boolean = {
    val event = new RobotPlaceInAirEvent(agent)
    MinecraftForge.EVENT_BUS.post(event)
    event.isAllowed
  }

  def onWorldInteraction(context: Context, duration: Double): Unit = {
    context.pause(duration)
  }

  // ----------------------------------------------------------------------- //

  @Callback(doc = "function():string -- Get the name of the agent.")
  def name(context: Context, args: Arguments): Array[AnyRef] = result(agent.name)

  @Callback(doc = "function(side:number[, face:number=side[, sneaky:boolean=false]]):boolean, string -- Perform a 'left click' towards the specified side. The `face' allows a more precise click calibration, and is relative to the targeted blockspace.")
  def swing(context: Context, args: Arguments): Array[AnyRef] = {
    // Swing the equipped tool (left click).
    val facing = checkSideForAction(args, 0)
    val sides =
      if (args.isInteger(1)) {
        Iterable(checkSideForFace(args, 1, facing))
      }
      else {
        // Always try the direction we're looking first.
        Iterable(facing) ++ EnumFacing.values.filter(side => side != facing && side != facing.getOpposite).toIterable
      }
    val sneaky = args.isBoolean(2) && args.checkBoolean(2)

    def triggerDelay(delay: Double = Settings.get.swingDelay) = {
      onWorldInteraction(context, delay)
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
    def click(player: Player, pos: BlockPos, side: EnumFacing) = {
      val breakTime = player.clickBlock(pos, side)
      val broke = breakTime > 0
      if (broke) {
        triggerDelay(breakTime)
      }
      (broke, "block")
    }

    var reason: Option[String] = None
    for (side <- sides) {
      val player = rotatedPlayer(facing, side)
      player.setSneaking(sneaky)

      val (success, what) = {
        val hit = pick(player, Settings.get.swingRange)
        (Option(hit) match {
          case Some(info) => info.typeOfHit
          case _ => RayTraceResult.Type.MISS
        }) match {
          case RayTraceResult.Type.ENTITY =>
            attack(player, hit.entityHit)
          case RayTraceResult.Type.BLOCK =>
            click(player, hit.getBlockPos, hit.sideHit)
          case _ =>
            // Retry with full block bounds, disregarding swing range.
            player.closestEntity(classOf[EntityLivingBase]) match {
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

    // all side attempts failed - but there could be a partial block that is hard to "see"
    val (hasBlock, _) = blockContent(facing)
    if (hasBlock) {
      val blockPos = position.offset(facing)
      val player = rotatedPlayer(facing, facing)
      player.setSneaking(sneaky)
      val (ok, why) = click(player, blockPos.toBlockPos, facing)
      player.setSneaking(false)
      return result(ok, why)
    }

    result(false, reason.orNull)
  }

  @Callback(doc = "function(side:number[, face:number=side[, sneaky:boolean=false[, duration:number=0]]]):boolean, string -- Perform a 'right click' towards the specified side. The `face' allows a more precise click calibration, and is relative to the targeted blockspace.")
  def use(context: Context, args: Arguments): Array[AnyRef] = {
    val facing = checkSideForAction(args, 0)
    val sides =
      if (args.isInteger(1)) {
        Iterable(checkSideForFace(args, 1, facing))
      }
      else {
        // Always try the direction we're looking first.
        Iterable(facing) ++ EnumFacing.values.filter(side => side != facing && side != facing.getOpposite).toIterable
      }
    val sneaky = args.isBoolean(2) && args.checkBoolean(2)
    val duration =
      if (args.isDouble(3)) args.checkDouble(3)
      else 0.0

    def triggerDelay() {
      onWorldInteraction(context, Settings.get.useDelay)
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
      val result = player.interactOn(entity, EnumHand.MAIN_HAND)
      endConsumeDrops(player, entity)
      result
    }

    for (side <- sides) {
      val player = rotatedPlayer(facing, side)
      player.setSneaking(sneaky)

      val (success, what) = Option(pick(player, Settings.get.useAndPlaceRange)) match {
        case Some(hit) if hit.typeOfHit == RayTraceResult.Type.ENTITY && interact(player, hit.entityHit) == EnumActionResult.SUCCESS =>
          triggerDelay()
          (true, "item_interacted")
        case Some(hit) if hit.typeOfHit == RayTraceResult.Type.BLOCK =>
          val (blockPos, hx, hy, hz) = clickParamsFromHit(hit)
          activationResult(player.activateBlockOrUseItem(blockPos, hit.sideHit, hx, hy, hz, duration))
        case _ =>
          (if (canPlaceInAir) {
            val (blockPos, hx, hy, hz) = clickParamsForPlace(facing)
            if (player.placeBlock(0, blockPos, facing, hx, hy, hz))
              ActivationType.ItemPlaced
            else {
              val (blockPos, hx, hy, hz) = clickParamsForItemUse(facing, side)
              player.activateBlockOrUseItem(blockPos, side.getOpposite, hx, hy, hz, duration)
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

  @Callback(doc = "function(side:number[, face:number=side[, sneaky:boolean=false]]):boolean -- Place a block towards the specified side. The `face' allows a more precise click calibration, and is relative to the targeted blockspace.")
  def place(context: Context, args: Arguments): Array[AnyRef] = {
    val facing = checkSideForAction(args, 0)
    val sides =
      if (args.isInteger(1)) {
        Iterable(checkSideForFace(args, 1, facing))
      }
      else {
        // Always try the direction we're looking first.
        Iterable(facing) ++ EnumFacing.values.filter(side => side != facing && side != facing.getOpposite).toIterable
      }
    val sneaky = args.isBoolean(2) && args.checkBoolean(2)
    val stack = agent.mainInventory.getStackInSlot(agent.selectedSlot)
    if (stack.isEmpty) {
      return result(Unit, "nothing selected")
    }

    for (side <- sides) {
      val player = rotatedPlayer(facing, side)
      player.setSneaking(sneaky)
      val success = Option(pick(player, Settings.get.useAndPlaceRange)) match {
        case Some(hit) if hit.typeOfHit == RayTraceResult.Type.BLOCK =>
          val (blockPos, hx, hy, hz) = clickParamsFromHit(hit)
          player.placeBlock(agent.selectedSlot, blockPos, hit.sideHit, hx, hy, hz)
        case None if canPlaceInAir && player.closestEntity(classOf[Entity]).isEmpty =>
          val (blockPos, hx, hy, hz) = clickParamsForPlace(facing)
          // blockPos here is the position of the agent
          // When a robot uses angel placement, the ItemBlock code offsets the pos to EnumFacing
          // but for a drone, the block at its position is air, which is replaceable, and thus
          // ItemBlock does not offset the position. We can do it here to correct that, and the code
          // here is still correct for the robot's use case
          val adjustedPos = blockPos.offset(facing)
          player.placeBlock(agent.selectedSlot, adjustedPos, facing, hx, hy, hz)
        case _ => false
      }
      player.setSneaking(false)
      if (success) {
        onWorldInteraction(context, Settings.get.placeDelay)
        return result(true)
      }
    }

    result(false)
  }

  // ----------------------------------------------------------------------- //

  protected def beginConsumeDrops(entity: Entity) {
    entity.captureDrops = true
  }

  protected def endConsumeDrops(player: Player, entity: Entity) {
    entity.captureDrops = false
    for (drop <- entity.capturedDrops) {
      val stack = drop.getItem
      InventoryUtils.addToPlayerInventory(stack, player)
    }
    entity.capturedDrops.clear()
  }

  // ----------------------------------------------------------------------- //

  protected def checkSideForFace(args: Arguments, n: Int, facing: EnumFacing): EnumFacing = agent.toGlobal(args.checkSideForFace(n, agent.toLocal(facing)))

  protected def pick(player: Player, range: Double): RayTraceResult = {
    val origin = new Vec3d(
      player.posX + player.facing.getFrontOffsetX * 0.5,
      player.posY + player.facing.getFrontOffsetY * 0.5,
      player.posZ + player.facing.getFrontOffsetZ * 0.5)
    val blockCenter = origin.addVector(
      player.facing.getFrontOffsetX * 0.51,
      player.facing.getFrontOffsetY * 0.51,
      player.facing.getFrontOffsetZ * 0.51)
    val target = blockCenter.addVector(
      player.side.getFrontOffsetX * range,
      player.side.getFrontOffsetY * range,
      player.side.getFrontOffsetZ * range)
    val hit = world.rayTraceBlocks(origin, target)
    player.closestEntity(classOf[Entity]) match {
      case Some(entity@(_: EntityLivingBase | _: EntityMinecart | _: entity.Drone)) if hit == null || new Vec3d(player.posX, player.posY, player.posZ).distanceTo(hit.hitVec) > player.getDistance(entity) => new RayTraceResult(entity)
      case _ => hit
    }
  }

  protected def clickParamsFromHit(hit: RayTraceResult): (BlockPos, Float, Float, Float) = {
    (hit.getBlockPos,
      (hit.hitVec.x - hit.getBlockPos.getX).toFloat,
      (hit.hitVec.y - hit.getBlockPos.getY).toFloat,
      (hit.hitVec.z - hit.getBlockPos.getZ).toFloat)
  }

  protected def clickParamsForItemUse(facing: EnumFacing, side: EnumFacing): (BlockPos, Float, Float, Float) = {
    val blockPos = position.offset(facing).offset(side)
    (blockPos.toBlockPos,
      0.5f - side.getFrontOffsetX * 0.5f,
      0.5f - side.getFrontOffsetY * 0.5f,
      0.5f - side.getFrontOffsetZ * 0.5f)
  }

  protected def clickParamsForPlace(facing: EnumFacing): (BlockPos, Float, Float, Float) = {
    (position.toBlockPos,
      0.5f + facing.getFrontOffsetX * 0.5f,
      0.5f + facing.getFrontOffsetY * 0.5f,
      0.5f + facing.getFrontOffsetZ * 0.5f)
  }
}
