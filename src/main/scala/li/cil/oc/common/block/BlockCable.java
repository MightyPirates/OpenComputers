package li.cil.oc.common.block;

import li.cil.oc.api.tileentity.Colored;
import li.cil.oc.common.capabilities.CapabilityColored;
import li.cil.oc.common.capabilities.CapabilityEnvironment;
import li.cil.oc.common.tileentity.TileEntityCable;
import li.cil.tis3d.util.WorldUtils;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

import javax.annotation.Nullable;

public final class BlockCable extends AbstractBlock {
    // ----------------------------------------------------------------------- //
    // AbstractBlock

    @Nullable
    @Override
    protected Class<? extends TileEntity> getTileEntityClass() {
        return TileEntityCable.class;
    }

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

    @SuppressWarnings("deprecation")
    @Override
    public boolean shouldSideBeRendered(final IBlockState state, final IBlockAccess world, final BlockPos pos, final EnumFacing side) {
        return true;
    }

    @Override
    public boolean isSideSolid(final IBlockState state, final IBlockAccess world, final BlockPos pos, final EnumFacing side) {
        return false;
    }

    @SuppressWarnings("deprecation")
    @Override
    public AxisAlignedBB getBoundingBox(final IBlockState state, final IBlockAccess world, final BlockPos pos) {
        return computeBounds(world, pos);
    }

    // ----------------------------------------------------------------------- //

    private static final AxisAlignedBB[] PRECOMPUTED_BOUNDS = new AxisAlignedBB[0b111111];

    static {
        for (int mask = 0; mask < PRECOMPUTED_BOUNDS.length; mask++) {
            double minX = 0.375;
            double minY = 0.375;
            double minZ = 0.375;
            double maxX = 0.625;
            double maxY = 0.625;
            double maxZ = 0.625;
            for (final EnumFacing side : EnumFacing.VALUES) {
                final boolean isSideConnected = ((1 << side.getIndex()) & mask) != 0;
                if (isSideConnected) {
                    if (side.getFrontOffsetX() < 0) minX = 0;
                    if (side.getFrontOffsetX() > 0) maxX = 1;
                    if (side.getFrontOffsetY() < 0) minY = 0;
                    if (side.getFrontOffsetY() > 0) maxY = 1;
                    if (side.getFrontOffsetZ() < 0) minZ = 0;
                    if (side.getFrontOffsetZ() > 0) maxZ = 1;
                }
            }
            PRECOMPUTED_BOUNDS[mask] = new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);
        }
    }

    public static int computeMask(final IBlockAccess world, final BlockPos pos) {
        int mask = 0;
        final TileEntity thisTileEntity = WorldUtils.getTileEntityThreadsafe(world, pos);
        if (thisTileEntity != null) {
            for (final EnumFacing side : EnumFacing.VALUES) {
                final TileEntity thatTileEntity = WorldUtils.getTileEntityThreadsafe(world, pos.offset(side));
                if (thatTileEntity != null) {
                    if (!thatTileEntity.hasCapability(CapabilityEnvironment.ENVIRONMENT_CAPABILITY, side.getOpposite())) {
                        continue;
                    }

                    final Colored thisColored = thisTileEntity.getCapability(CapabilityColored.COLORED_CAPABILITY, side);
                    final Colored thatColored = thatTileEntity.getCapability(CapabilityColored.COLORED_CAPABILITY, side.getOpposite());
                    if (thisColored != null && thatColored != null && thisColored.controlsConnectivity() && thatColored.controlsConnectivity() && thisColored.getColor() != thatColored.getColor()) {
                        continue;
                    }

                    mask = addToMask(side, mask);
                }
            }
        }
        return mask;
    }

    public static AxisAlignedBB computeBounds(final IBlockAccess world, final BlockPos pos) {
        return PRECOMPUTED_BOUNDS[computeMask(world, pos)];
    }

    private static int addToMask(final EnumFacing side, final int mask) {
        return mask | (1 << side.getIndex());
    }
}
