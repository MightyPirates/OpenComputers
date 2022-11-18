package li.cil.oc.common.block

import li.cil.oc.common.container.ContainerTypes
import li.cil.oc.common.tileentity
import li.cil.oc.integration.util.Wrench
import net.minecraft.block.AbstractBlock.Properties
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.ServerPlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.util.Direction
import net.minecraft.util.Hand
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IBlockReader
import net.minecraft.world.IWorldReader
import net.minecraft.world.World

class Adapter(props: Properties) extends SimpleBlock(props) with traits.GUI {
  override def openGui(player: ServerPlayerEntity, world: World, pos: BlockPos): Unit = world.getBlockEntity(pos) match {
    case te: tileentity.Adapter => ContainerTypes.openAdapterGui(player, te)
    case _ =>
  }

  override def newBlockEntity(world: IBlockReader) = new tileentity.Adapter(tileentity.TileEntityTypes.ADAPTER)

  // ----------------------------------------------------------------------- //

  @Deprecated
  override def neighborChanged(state: BlockState, world: World, pos: BlockPos, block: Block, fromPos: BlockPos, b: Boolean): Unit =
    world.getBlockEntity(pos) match {
      case adapter: tileentity.Adapter => adapter.neighborChanged()
      case _ => // Ignore.
    }

  override def onNeighborChange(state: BlockState, world: IWorldReader, pos: BlockPos, neighbor: BlockPos) =
    world.getBlockEntity(pos) match {
      case adapter: tileentity.Adapter =>
        // TODO can we just pass the blockpos?
        val side =
          if (neighbor == (pos.below():BlockPos)) Direction.DOWN
          else if (neighbor == (pos.above():BlockPos)) Direction.UP
          else if (neighbor == pos.north()) Direction.NORTH
          else if (neighbor == pos.south()) Direction.SOUTH
          else if (neighbor == pos.west()) Direction.WEST
          else if (neighbor == pos.east()) Direction.EAST
          else throw new IllegalArgumentException("not a neighbor") // TODO wat
        adapter.neighborChanged(side)
      case _ => // Ignore.
    }

  override def localOnBlockActivated(world: World, pos: BlockPos, player: PlayerEntity, hand: Hand, heldItem: ItemStack, side: Direction, hitX: Float, hitY: Float, hitZ: Float): Boolean = {
    if (Wrench.holdsApplicableWrench(player, pos)) {
      val sideToToggle = if (player.isCrouching) side.getOpposite else side
      world.getBlockEntity(pos) match {
        case adapter: tileentity.Adapter =>
          if (!world.isClientSide) {
            val oldValue = adapter.openSides(sideToToggle.ordinal())
            adapter.setSideOpen(sideToToggle, !oldValue)
          }
          true
        case _ => false
      }
    }
    else super.localOnBlockActivated(world, pos, player, hand, heldItem, side, hitX, hitY, hitZ)
  }
}
