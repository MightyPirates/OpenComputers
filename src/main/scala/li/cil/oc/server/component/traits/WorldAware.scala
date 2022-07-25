package li.cil.oc.server.component.traits

import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedBlock._
import li.cil.oc.util.ExtendedWorld._
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.item.minecart.MinecartEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.Direction
import net.minecraft.util.Hand
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockRayTraceResult
import net.minecraft.util.math.shapes.ISelectionContext
import net.minecraft.world.server.ServerWorld
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.common.util.FakePlayerFactory
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.event.world.BlockEvent
import net.minecraftforge.eventbus.api.Event.Result
import net.minecraftforge.fluids.IFluidBlock
import net.minecraftforge.items.IItemHandler
import net.minecraftforge.items.wrapper.InvWrapper

import scala.collection.convert.ImplicitConversionsToScala._

trait WorldAware {
  def position: BlockPosition

  def world = position.world.get

  def fakePlayer: PlayerEntity = {
    val player = FakePlayerFactory.get(world.asInstanceOf[ServerWorld], Settings.get.fakePlayerProfile)
    player.setPos(position.x + 0.5, position.y + 0.5, position.z + 0.5)
    player
  }

  def mayInteract(blockPos: BlockPosition, face: Direction): Boolean = {
    try {
      val trace = new BlockRayTraceResult(fakePlayer.position, face, blockPos.toBlockPos, false)
      val event = new PlayerInteractEvent.RightClickBlock(fakePlayer, Hand.MAIN_HAND, blockPos.toBlockPos, trace)
      MinecraftForge.EVENT_BUS.post(event)
      !event.isCanceled && event.getUseBlock != Result.DENY
    } catch {
      case t: Throwable =>
        OpenComputers.log.warn("Some event handler threw up while checking for permission to access a block.", t)
        true
    }
  }

  def mayInteract(blockPos: BlockPosition, side: Direction, inventory: IItemHandler): Boolean = mayInteract(blockPos, side) && (inventory match {
    case inv: InvWrapper if inv.getInv != null => inv.getInv.stillValid(fakePlayer)
    case _ => true
  })

  def entitiesInBounds[Type <: Entity](clazz: Class[Type], bounds: AxisAlignedBB) = {
    world.getEntitiesOfClass(clazz, bounds)
  }

  def entitiesInBlock[Type <: Entity](clazz: Class[Type], blockPos: BlockPosition) = {
    entitiesInBounds(clazz, blockPos.bounds)
  }

  def entitiesOnSide[Type <: Entity](clazz: Class[Type], side: Direction) = {
    entitiesInBlock(clazz, position.offset(side))
  }

  def closestEntity[Type <: Entity](clazz: Class[Type], side: Direction) = {
    val blockPos = position.offset(side)
    val candidates = world.getEntitiesOfClass(clazz, blockPos.bounds, null)
    if (!candidates.isEmpty) Some(candidates.minBy(e => fakePlayer.distanceToSqr(e))) else None
  }

  def blockContent(side: Direction) = {
    closestEntity[Entity](classOf[Entity], side) match {
      case Some(_@(_: LivingEntity | _: MinecartEntity)) =>
        (true, "entity")
      case _ =>
        val blockPos = position.offset(side)
        val state = world.getBlockState(blockPos.toBlockPos)
        val block = state.getBlock
        if (block.isAir(state, world, blockPos.toBlockPos)) {
          (false, "air")
        }
        else if (!block.isInstanceOf[IFluidBlock]) {
          val event = new BlockEvent.BreakEvent(world, blockPos.toBlockPos, state, fakePlayer)
          MinecraftForge.EVENT_BUS.post(event)
          (event.isCanceled, "liquid")
        }
        else if (block.isReplaceable(blockPos)) {
          val event = new BlockEvent.BreakEvent(world, blockPos.toBlockPos, state, fakePlayer)
          MinecraftForge.EVENT_BUS.post(event)
          (event.isCanceled, "replaceable")
        }
        else if (state.getCollisionShape(world, blockPos.toBlockPos, ISelectionContext.empty).isEmpty) {
          (true, "passable")
        }
        else {
          (true, "solid")
        }
    }
  }
}
