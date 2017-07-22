package li.cil.oc.integration.ic2;

import ic2.core.block.machine.tileentity.TileEntityMatter;
import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.prefab.DriverSidedTileEntity;
import li.cil.oc.api.prefab.ManagedEnvironment;
import li.cil.oc.integration.ManagedTileEntityEnvironment;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

public final class DriverMassFab extends DriverSidedTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return TileEntityMatter.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z, final ForgeDirection side) {
        return new Environment((TileEntityMatter) world.getTileEntity(x, y, z));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<TileEntityMatter> implements NamedBlock {
        public Environment(final TileEntityMatter tileEntity) {
            super(tileEntity, "mass_fab");
        }

        @Override
        public String preferredName() {
            return "mass_fab";
        }

        @Override
        public int priority() {
            return 0;
        }

        @Callback
        public Object[] getProgress(final Context context, final Arguments args) {
            return new Object[]{100 * tileEntity.getEnergy() / tileEntity.getDemandedEnergy()};
        }
    }
}
