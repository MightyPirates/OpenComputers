package li.cil.oc.common.block.traits

import java.util

import li.cil.oc.common.block.SimpleBlock
import net.minecraft.block.BlockState
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.fluid.FluidState
import net.minecraft.item.ItemStack
import net.minecraft.loot.LootContext
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IBlockReader
import net.minecraft.world.World

import scala.reflect.ClassTag

trait CustomDrops[Tile <: TileEntity] extends SimpleBlock {
  protected def tileTag: ClassTag[Tile]

  override def getDrops(state: BlockState, ctx: LootContext.Builder): util.List[ItemStack] = new util.ArrayList[ItemStack]()

  @Deprecated
  override def onRemove(state: BlockState, world: World, pos: BlockPos, newState: BlockState, moved: Boolean): Unit = {
    // Copied from vanilla, can't use super as that also drops the contents.
    if (state.hasTileEntity && (!state.is(newState.getBlock) || !newState.hasTileEntity)) {
      world.removeBlockEntity(pos)
    }
  }

  override def removedByPlayer(state: BlockState, world: World, pos: BlockPos, player: PlayerEntity, willHarvest: Boolean, fluid: FluidState): Boolean = {
    if (!world.isClientSide) {
      val matcher = tileTag
      world.getBlockEntity(pos) match {
        case matcher(tileEntity) => doCustomDrops(tileEntity, player, willHarvest)
        case _ =>
      }
    }
    super.removedByPlayer(state, world, pos, player, willHarvest, fluid)
  }

  override def setPlacedBy(world: World, pos: BlockPos, state: BlockState, placer: LivingEntity, stack: ItemStack): Unit = {
    super.setPlacedBy(world, pos, state, placer, stack)
    val matcher = tileTag
    world.getBlockEntity(pos) match {
      case matcher(tileEntity) => doCustomInit(tileEntity, placer, stack)
      case _ =>
    }
  }

  protected def doCustomInit(tileEntity: Tile, player: LivingEntity, stack: ItemStack): Unit = {}

  protected def doCustomDrops(tileEntity: Tile, player: PlayerEntity, willHarvest: Boolean): Unit = {}
}
