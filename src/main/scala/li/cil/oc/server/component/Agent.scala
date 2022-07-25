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
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.Pose
import net.minecraft.entity.item.ItemEntity
import net.minecraft.entity.item.minecart.MinecartEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.IInventory
import net.minecraft.util.Direction
import net.minecraft.util.Hand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.BlockRayTraceResult
import net.minecraft.util.math.EntityRayTraceResult
import net.minecraft.util.math.RayTraceContext
import net.minecraft.util.math.RayTraceResult
import net.minecraft.util.math.vector.Vector3d
import net.minecraftforge.common.MinecraftForge

import scala.collection.convert.WrapAsScala._

trait Agent extends traits.WorldControl with traits.InventoryControl with traits.InventoryWorldControl with traits.TankAware with traits.TankControl with traits.TankWorldControl {
  def agent: internal.Agent

  override def position = BlockPosition(agent)

  override def fakePlayer: PlayerEntity = agent.player

  protected def rotatedPlayer(facing: Direction = agent.facing, side: Direction = agent.facing): Player = {
    val player = agent.player.asInstanceOf[Player]
    Player.updatePositionAndRotation(player, facing, side)
    // no need to set inventory, calling agent.Player already did that
    //Player.setPlayerInventoryItems(player)
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
        Iterable(facing) ++ Direction.values.filter(side => side != facing && side != facing.getOpposite).toIterable
      }
    val sneaky = args.isBoolean(2) && args.checkBoolean(2)

    def triggerDelay(delay: Double = Settings.get.swingDelay) = {
      onWorldInteraction(context, delay)
    }
    def attack(player: Player, entity: Entity) = {
      beginConsumeDrops(entity)
      player.attack(entity)
      // Mine carts have to be hit quickly in succession to break, so we click
      // until it breaks. But avoid an infinite loop... you never know.
      entity match {
        case _: MinecartEntity => for (_ <- 0 until 10 if entity.isAlive) {
          player.attack(entity)
        }
        case _ =>
      }
      endConsumeDrops(player, entity)
      triggerDelay()
      (true, "entity")
    }
    def click(player: Player, pos: BlockPos, side: Direction) = {
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
      player.setPose(if (sneaky) Pose.CROUCHING else Pose.STANDING)

      val (success, what) = {
        val hit = pick(player, Settings.get.swingRange)
        (Option(hit) match {
          case Some(info) => info.getType
          case _ => RayTraceResult.Type.MISS
        }) match {
          case RayTraceResult.Type.ENTITY =>
            attack(player, hit.asInstanceOf[EntityRayTraceResult].getEntity)
          case RayTraceResult.Type.BLOCK =>
            val blockHit = hit.asInstanceOf[BlockRayTraceResult]
            click(player, blockHit.getBlockPos, blockHit.getDirection)
          case _ =>
            // Retry with full block bounds, disregarding swing range.
            player.closestEntity(classOf[LivingEntity]) match {
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

      player.setPose(Pose.STANDING)
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
      player.setPose(if (sneaky) Pose.CROUCHING else Pose.STANDING)
      val (ok, why) = click(player, blockPos.toBlockPos, facing)
      player.setPose(Pose.STANDING)
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
        Iterable(facing) ++ Direction.values.filter(side => side != facing && side != facing.getOpposite).toIterable
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
      val result = player.interactOn(entity, Hand.MAIN_HAND)
      endConsumeDrops(player, entity)
      result
    }

    for (side <- sides) {
      val player = rotatedPlayer(facing, side)
      player.setPose(if (sneaky) Pose.CROUCHING else Pose.STANDING)

      val (success, what) = Option(pick(player, Settings.get.useAndPlaceRange)) match {
        case Some(hit) if hit.getType == RayTraceResult.Type.ENTITY && interact(player, hit.asInstanceOf[EntityRayTraceResult].getEntity).consumesAction =>
          triggerDelay()
          (true, "item_interacted")
        case Some(hit) if hit.getType == RayTraceResult.Type.BLOCK =>
          val blockHit = hit.asInstanceOf[BlockRayTraceResult]
          val (blockPos, hx, hy, hz) = clickParamsFromHit(blockHit)
          activationResult(player.activateBlockOrUseItem(blockPos, blockHit.getDirection, hx, hy, hz, duration))
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

      player.setPose(Pose.STANDING)
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
        Iterable(facing) ++ Direction.values.filter(side => side != facing && side != facing.getOpposite).toIterable
      }
    val sneaky = args.isBoolean(2) && args.checkBoolean(2)
    val stack = agent.mainInventory.getItem(agent.selectedSlot)
    if (stack.isEmpty) {
      return result(Unit, "nothing selected")
    }

    for (side <- sides) {
      val player = rotatedPlayer(facing, side)
      player.setPose(if (sneaky) Pose.CROUCHING else Pose.STANDING)
      val success = Option(pick(player, Settings.get.useAndPlaceRange)) match {
        case Some(hit) if hit.getType == RayTraceResult.Type.BLOCK =>
          val blockHit = hit.asInstanceOf[BlockRayTraceResult]
          val (blockPos, hx, hy, hz) = clickParamsFromHit(blockHit)
          player.placeBlock(agent.selectedSlot, blockPos, blockHit.getDirection, hx, hy, hz)
        case None if canPlaceInAir && player.closestEntity(classOf[Entity]).isEmpty =>
          val (blockPos, hx, hy, hz) = clickParamsForPlace(facing)
          // blockPos here is the position of the agent
          // When a robot uses angel placement, the BlockItem code offsets the pos to Direction
          // but for a drone, the block at its position is air, which is replaceable, and thus
          // BlockItem does not offset the position. We can do it here to correct that, and the code
          // here is still correct for the robot's use case
          val adjustedPos: BlockPos = blockPos.relative(facing)
          // adjustedPos is the position we want to place the block
          // but onItemUse will try to adjust the placement if the target position is not replaceable
          // we don't want that
          val state: BlockState = world.getBlockState(adjustedPos)
          if (state.getMaterial.isReplaceable) {
            player.placeBlock(agent.selectedSlot, adjustedPos, facing, hx, hy, hz)
          } else {
            false
          }
        case _ => false
      }
      player.setPose(Pose.STANDING)
      if (success) {
        onWorldInteraction(context, Settings.get.placeDelay)
        return result(true)
      }
    }

    result(false)
  }

  // ----------------------------------------------------------------------- //

  protected def beginConsumeDrops(entity: Entity) {
    entity.captureDrops(new java.util.ArrayList[ItemEntity]())
  }


  protected def endConsumeDrops(player: Player, entity: Entity) {
    val captured = entity.captureDrops(null)
    // this inventory size check is a HACK to preserve old behavior that a agent can suck items out
    // of the capturedDrops. Ideally, we'd only pick up items off the ground. We could clear the
    // capturedDrops when Player.attack() is called
    // But this felt slightly less hacky, slightly
    if (player.inventory.getContainerSize > 0) {
      for (drop <- captured) {
        if (drop.isAlive) {
          val stack = drop.getItem
          InventoryUtils.addToPlayerInventory(stack, player, spawnInWorld = false)
        }
      }
    }
  }

  // ----------------------------------------------------------------------- //

  protected def checkSideForFace(args: Arguments, n: Int, facing: Direction): Direction = agent.toGlobal(args.checkSideForFace(n, agent.toLocal(facing)))

  protected def pick(player: Player, range: Double): RayTraceResult = {
    val origin = new Vector3d(
      player.getX + player.facing.getStepX * 0.5,
      player.getY + player.facing.getStepY * 0.5,
      player.getZ + player.facing.getStepZ * 0.5)
    val blockCenter = origin.add(
      player.facing.getStepX * 0.51,
      player.facing.getStepY * 0.51,
      player.facing.getStepZ * 0.51)
    val target = blockCenter.add(
      player.side.getStepX * range,
      player.side.getStepY * range,
      player.side.getStepZ * range)
    val hit = world.clip(new RayTraceContext(origin, target, RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.ANY, player))
    player.closestEntity(classOf[Entity]) match {
      case Some(entity@(_: LivingEntity | _: MinecartEntity | _: entity.Drone)) if hit.getType == RayTraceResult.Type.MISS || player.distanceToSqr(hit.getLocation) > player.distanceToSqr(entity) => new EntityRayTraceResult(entity)
      case _ => hit
    }
  }

  protected def clickParamsFromHit(hit: BlockRayTraceResult): (BlockPos, Float, Float, Float) = {
    (hit.getBlockPos,
      (hit.getLocation.x - hit.getBlockPos.getX).toFloat,
      (hit.getLocation.y - hit.getBlockPos.getY).toFloat,
      (hit.getLocation.z - hit.getBlockPos.getZ).toFloat)
  }

  protected def clickParamsForItemUse(facing: Direction, side: Direction): (BlockPos, Float, Float, Float) = {
    val blockPos = position.offset(facing).offset(side)
    (blockPos.toBlockPos,
      0.5f - side.getStepX * 0.5f,
      0.5f - side.getStepY * 0.5f,
      0.5f - side.getStepZ * 0.5f)
  }

  protected def clickParamsForPlace(facing: Direction): (BlockPos, Float, Float, Float) = {
    (position.toBlockPos,
      0.5f + facing.getStepX * 0.5f,
      0.5f + facing.getStepY * 0.5f,
      0.5f + facing.getStepZ * 0.5f)
  }
}
