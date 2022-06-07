package li.cil.oc.common.block

import li.cil.oc.OpenComputers
import li.cil.oc.common.GuiType
import li.cil.oc.common.block.property.{PropertyRotatable, PropertyTile}
import li.cil.oc.common.tileentity
import net.minecraft.block.state.BlockStateContainer
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import net.minecraft.world.{IBlockAccess, World}
import net.minecraftforge.common.property.{ExtendedBlockState, IExtendedBlockState}

class Waypoint extends RedstoneAware {
  override def createBlockState() = new ExtendedBlockState(this, Array(PropertyRotatable.Pitch, PropertyRotatable.Yaw), Array(PropertyTile.Tile))

  override def getMetaFromState(state: IBlockState): Int = (state.getValue(PropertyRotatable.Pitch).ordinal() << 2) | state.getValue(PropertyRotatable.Yaw).getHorizontalIndex

  override def getStateFromMeta(meta: Int): IBlockState = getDefaultState.withProperty(PropertyRotatable.Pitch, EnumFacing.byIndex(meta >> 2)).withProperty(PropertyRotatable.Yaw, EnumFacing.byHorizontalIndex(meta & 0x3))

  override def getExtendedState(state: IBlockState, world: IBlockAccess, pos: BlockPos): IBlockState =
    (state, world.getTileEntity(pos)) match {
      case (extendedState: IExtendedBlockState, tile: tileentity.Screen) =>
        extendedState.
          withProperty(property.PropertyTile.Tile, tile).
          withProperty(PropertyRotatable.Pitch, tile.pitch).
          withProperty(PropertyRotatable.Yaw, tile.yaw)
      case _ => state
    }

  // ----------------------------------------------------------------------- //

  override def createNewTileEntity(world: World, metadata: Int) = new tileentity.Waypoint()

  // ----------------------------------------------------------------------- //

  override def localOnBlockActivated(world: World, pos: BlockPos, player: EntityPlayer, hand: EnumHand, heldItem: ItemStack, side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float): Boolean = {
    if (!player.isSneaking) {
      if (world.isRemote) {
        player.openGui(OpenComputers, GuiType.Waypoint.id, world, pos.getX, pos.getY, pos.getZ)
      }
      true
    }
    else super.localOnBlockActivated(world, pos, player, hand, heldItem, side, hitX, hitY, hitZ)
  }

  override def getValidRotations(world: World, pos: BlockPos): Array[EnumFacing] =
    world.getTileEntity(pos) match {
      case waypoint: tileentity.Waypoint =>
        EnumFacing.values.filter {
          d => d != waypoint.facing && d != waypoint.facing.getOpposite
        }
      case _ => super.getValidRotations(world, pos)
    }
}
