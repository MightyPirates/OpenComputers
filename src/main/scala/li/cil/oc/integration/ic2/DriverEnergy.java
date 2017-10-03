package li.cil.oc.integration.ic2;

import ic2.core.block.TileEntityBlock;
import ic2.core.block.comp.Energy;
import li.cil.oc.api.driver.DriverBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.integration.ManagedTileEntityEnvironment;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

public final class DriverEnergy implements DriverBlock {
    @Override
    public boolean worksWith(final World world, final BlockPos pos, final EnumFacing side) {
        final TileEntity tileEntity = world.getTileEntity(pos);
        if (tileEntity instanceof TileEntityBlock) {
            final TileEntityBlock tileEntityBlock = (TileEntityBlock) tileEntity;
            return tileEntityBlock.hasComponent(Energy.class);
        }
        return false;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final BlockPos pos, final EnumFacing side) {
        return new Environment((TileEntityBlock) world.getTileEntity(pos));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<TileEntityBlock> {
        public Environment(final TileEntityBlock tileEntity) {
            super(tileEntity, "ic2_energy");
        }

        @Callback
        public Object[] getCapacity(final Context context, final Arguments args) {
            final Energy energy = tileEntity.getComponent(Energy.class);
            return new Object[]{energy.getCapacity()};
        }

        @Callback
        public Object[] getEnergy(final Context context, final Arguments args) {
            final Energy energy = tileEntity.getComponent(Energy.class);
            return new Object[]{energy.getEnergy()};
        }

        @Callback
        public Object[] getSinkTier(final Context context, final Arguments args) {
            final Energy energy = tileEntity.getComponent(Energy.class);
            return new Object[]{energy.getSinkTier()};
        }

        @Callback
        public Object[] getSourceTier(final Context context, final Arguments args) {
            final Energy energy = tileEntity.getComponent(Energy.class);
            return new Object[]{energy.getSourceTier()};
        }
    }
}
