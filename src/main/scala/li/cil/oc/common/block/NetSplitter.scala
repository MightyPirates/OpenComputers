package li.cil.oc.common.block

import li.cil.oc.common.block.property.PropertyTile
import li.cil.oc.common.tileentity
import li.cil.oc.integration.util.Wrench
import net.minecraft.block.state.BlockState
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import net.minecraftforge.common.property.ExtendedBlockState
import net.minecraftforge.common.property.IExtendedBlockState

class NetSplitter extends RedstoneAware {
  override def createBlockState(): BlockState = new ExtendedBlockState(this, Array.empty, Array(PropertyTile.Tile))

  override def getExtendedState(state: IBlockState, world: IBlockAccess, pos: BlockPos): IBlockState =
    (state, world.getTileEntity(pos)) match {
      case (extendedState: IExtendedBlockState, t: tileentity.NetSplitter) =>
        extendedState.withProperty(property.PropertyTile.Tile, t)
      case _ => state
    }

  // ----------------------------------------------------------------------- //

  override def isSideSolid(world: IBlockAccess, pos: BlockPos, side: EnumFacing): Boolean = false

  // ----------------------------------------------------------------------- //

  override def createNewTileEntity(world: World, meta: Int) = new tileentity.NetSplitter()

  // ----------------------------------------------------------------------- //

  // NOTE: must not be final for immibis microblocks to work.
  override def onBlockActivated(world: World, pos: BlockPos, state: IBlockState, player: EntityPlayer, side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float): Boolean = {
    if (Wrench.holdsApplicableWrench(player, pos)) {
      val sideToToggle = if (player.isSneaking) side.getOpposite else side
      world.getTileEntity(pos) match {
        case splitter: tileentity.NetSplitter =>
          if (!world.isRemote) {
            val oldValue = splitter.openSides(sideToToggle.ordinal())
            splitter.setSideOpen(sideToToggle, !oldValue)
          }
          true
        case _ => false
      }
    }
    else super.onBlockActivated(world, pos, state, player, side, hitX, hitY, hitZ)
  }
}
