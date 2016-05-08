package li.cil.oc.common.block

import li.cil.oc.common.GuiType
import li.cil.oc.common.tileentity
import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World

class Adapter extends SimpleBlock with traits.GUI {
  override def guiType = GuiType.Adapter

  override def createNewTileEntity(world: World, metadata: Int) = new tileentity.Adapter()

  // ----------------------------------------------------------------------- //

  override def onNeighborBlockChange(world: World, pos: BlockPos, state: IBlockState, neighborBlock: Block) =
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
}
