package li.cil.oc.server.component.traits

import li.cil.oc.Settings
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedBlock._
import li.cil.oc.util.ExtendedWorld._
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityMinecart
import net.minecraft.util.AxisAlignedBB
import net.minecraft.world.WorldServer
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.common.util.FakePlayer
import net.minecraftforge.common.util.FakePlayerFactory
import net.minecraftforge.common.util.ForgeDirection
import net.minecraftforge.event.world.BlockEvent
import net.minecraftforge.fluids.FluidRegistry

import scala.collection.convert.WrapAsScala._
import scala.reflect.ClassTag
import scala.reflect.classTag

trait WorldAware {
  def position: BlockPosition

  def world = position.world.get

  def fakePlayer: FakePlayer = {
    val player = FakePlayerFactory.get(world.asInstanceOf[WorldServer], Settings.get.fakePlayerProfile)
    player.posX = position.x + 0.5
    player.posY = position.y + 0.5
    player.posZ = position.z + 0.5
    player
  }

  def entitiesInBounds[Type <: Entity : ClassTag](bounds: AxisAlignedBB) = {
    world.getEntitiesWithinAABB(classTag[Type].runtimeClass, bounds).map(_.asInstanceOf[Type])
  }

  def entitiesInBlock[Type <: Entity : ClassTag](blockPos: BlockPosition) = {
    entitiesInBounds[Type](blockPos.bounds)
  }

  def entitiesOnSide[Type <: Entity : ClassTag](side: ForgeDirection) = {
    entitiesInBlock[Type](position.offset(side))
  }

  def closestEntity[Type <: Entity : ClassTag](side: ForgeDirection) = {
    val blockPos = position.offset(side)
    Option(world.findNearestEntityWithinAABB(classTag[Type].runtimeClass, blockPos.bounds, fakePlayer)).map(_.asInstanceOf[Type])
  }

  def blockContent(side: ForgeDirection) = {
    closestEntity[Entity](side) match {
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
          val event = new BlockEvent.BreakEvent(blockPos.x, blockPos.y, blockPos.z, world, block, metadata, fakePlayer)
          MinecraftForge.EVENT_BUS.post(event)
          (event.isCanceled, "liquid")
        }
        else if (block.isReplaceable(blockPos)) {
          val event = new BlockEvent.BreakEvent(blockPos.x, blockPos.y, blockPos.z, world, block, metadata, fakePlayer)
          MinecraftForge.EVENT_BUS.post(event)
          (event.isCanceled, "replaceable")
        }
        else {
          (true, "solid")
        }
    }
  }
}
