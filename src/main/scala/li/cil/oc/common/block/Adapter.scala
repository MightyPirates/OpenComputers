package li.cil.oc.common.block

import li.cil.oc.common.GuiType
import li.cil.oc.common.tileentity
import li.cil.oc.integration.util.Wrench
import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World

class Adapter extends AbstractBlock with traits.GUI {
  override def guiType = GuiType.Adapter

  override def createNewTileEntity(world: World, metadata: Int) = new tileentity.Adapter()

  // ----------------------------------------------------------------------- //

  override def neighborChanged(state: IBlockState, world: World, pos: BlockPos, block: Block, fromPos: BlockPos): Unit =
    world.getTileEntity(pos) match {
      case adapter: tileentity.Adapter => adapter.neighborChanged()
      case _ => // Ignore.
    }

  override def onNeighborChange(world: IBlockAccess, pos: BlockPos, neighbor: BlockPos) =
    world.getTileEntity(pos) match {
      case adapter: tileentity.Adapter =>
        // TODO can we just pass the blockpos?
        val side =
          if (neighbor == pos.down()) EnumFacing.DOWN
          else if (neighbor == pos.up()) EnumFacing.UP
          else if (neighbor == pos.north()) EnumFacing.NORTH
          else if (neighbor == pos.south()) EnumFacing.SOUTH
          else if (neighbor == pos.west()) EnumFacing.WEST
          else if (neighbor == pos.east()) EnumFacing.EAST
          else throw new IllegalArgumentException("not a neighbor") // TODO wat
        adapter.neighborChanged(side)
      case _ => // Ignore.
    }

  override def localOnBlockActivated(world: World, pos: BlockPos, player: EntityPlayer, hand: EnumHand, heldItem: ItemStack, side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float): Boolean = {
    if (Wrench.holdsApplicableWrench(player, pos)) {
      val sideToToggle = if (player.isSneaking) side.getOpposite else side
      world.getTileEntity(pos) match {
        case adapter: tileentity.Adapter =>
          if (!world.isRemote) {
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
