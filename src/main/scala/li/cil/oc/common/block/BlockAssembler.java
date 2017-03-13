package li.cil.oc.common.block;

import li.cil.oc.common.tileentity.TileEntityAssembler;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

import javax.annotation.Nullable;

public final class BlockAssembler extends AbstractBlock {
    // ----------------------------------------------------------------------- //
    // Block

    @SuppressWarnings("deprecation")
    @Override
    public boolean isOpaqueCube(final IBlockState state) {
        return false;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean isFullCube(final IBlockState state) {
        return false;
    }

    @Override
    public boolean isBlockSolid(final IBlockAccess worldIn, final BlockPos pos, final EnumFacing side) {
        return side == EnumFacing.DOWN || side == EnumFacing.UP;
    }

    @Override
    public boolean isSideSolid(final IBlockState base_state, final IBlockAccess world, final BlockPos pos, final EnumFacing side) {
        return side == EnumFacing.DOWN || side == EnumFacing.UP;
    }

    // ----------------------------------------------------------------------- //
    // AbstractBlock

    @Nullable
    @Override
    protected Class<? extends TileEntity> getTileEntityClass() {
        return TileEntityAssembler.class;
    }
}
