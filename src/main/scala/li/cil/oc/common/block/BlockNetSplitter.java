package li.cil.oc.common.block;

import li.cil.oc.common.tileentity.TileEntityNetSplitter;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

import javax.annotation.Nullable;

public final class BlockNetSplitter extends AbstractBlock {
    @Nullable
    @Override
    protected Class<? extends TileEntity> getTileEntityClass() {
        return TileEntityNetSplitter.class;
    }

    // ----------------------------------------------------------------------- //

    @Override
    public boolean isSideSolid(final IBlockState base_state, final IBlockAccess world, final BlockPos pos, final EnumFacing side) {
        return false;
    }
}
