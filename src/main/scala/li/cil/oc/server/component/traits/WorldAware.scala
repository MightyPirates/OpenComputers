package li.cil.oc.server.component.traits

import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedBlock._
import li.cil.oc.util.ExtendedWorld._
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityMinecart
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.world.WorldServer
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.common.util.FakePlayerFactory
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.event.world.BlockEvent
import net.minecraftforge.fluids.FluidRegistry
import net.minecraftforge.fml.common.eventhandler.Event.Result

trait WorldAware {
  def position: BlockPosition

  def world = position.world.get

  def fakePlayer: EntityPlayer = {
    val player = FakePlayerFactory.get(world.asInstanceOf[WorldServer], Settings.get.fakePlayerProfile)
    player.posX = position.x + 0.5
    player.posY = position.y + 0.5
    player.posZ = position.z + 0.5
    player
  }

  def mayInteract(blockPos: BlockPosition, face: EnumFacing): Boolean = {
    try {
      val event = new PlayerInteractEvent.RightClickBlock(fakePlayer, EnumHand.MAIN_HAND, null, blockPos.toBlockPos, face, null)
      MinecraftForge.EVENT_BUS.post(event)
      !event.isCanceled && event.getUseBlock != Result.DENY
    } catch {
      case t: Throwable =>
        OpenComputers.log.warn("Some event handler threw up while checking for permission to access a block.", t)
        true
    }
  }

  def entitiesInBounds[Type <: Entity](clazz: Class[Type], bounds: AxisAlignedBB) = {
    world.getEntitiesWithinAABB(clazz, bounds)
  }

  def entitiesInBlock[Type <: Entity](clazz: Class[Type], blockPos: BlockPosition) = {
    entitiesInBounds(clazz, blockPos.bounds)
  }

  def entitiesOnSide[Type <: Entity](clazz: Class[Type], side: EnumFacing) = {
    entitiesInBlock(clazz, position.offset(side))
  }

  def closestEntity[Type <: Entity](clazz: Class[Type], side: EnumFacing) = {
    val blockPos = position.offset(side)
    Option(world.findNearestEntityWithinAABB(clazz, blockPos.bounds, fakePlayer))
  }

  def blockContent(side: EnumFacing) = {
    closestEntity[Entity](classOf[Entity], side) match {
      case Some(_@(_: EntityLivingBase | _: EntityMinecart)) =>
        (true, "entity")
      case _ =>
        val blockPos = position.offset(side)
        val block = world.getBlock(blockPos)
        val metadata = world.getBlockMetadata(blockPos)
        if (block.isAir(blockPos)) {
          (false, "air")
        }
        else if (FluidRegistry.lookupFluidForBlock(block) != null) {
          val event = new BlockEvent.BreakEvent(world, blockPos.toBlockPos, metadata, fakePlayer)
          MinecraftForge.EVENT_BUS.post(event)
          (event.isCanceled, "liquid")
        }
        else if (block.isReplaceable(blockPos)) {
          val event = new BlockEvent.BreakEvent(world, blockPos.toBlockPos, metadata, fakePlayer)
          MinecraftForge.EVENT_BUS.post(event)
          (event.isCanceled, "replaceable")
        }
        else if (block.getCollisionBoundingBoxFromPool(blockPos) == null) {
          (true, "passable")
        }
        else {
          (true, "solid")
        }
    }
  }
}
