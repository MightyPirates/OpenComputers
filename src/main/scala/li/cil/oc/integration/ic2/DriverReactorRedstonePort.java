package li.cil.oc.integration.ic2;

import ic2.api.reactor.IReactor;
import ic2.core.block.comp.FluidReactorLookup;
import ic2.core.block.reactor.tileentity.TileEntityReactorRedstonePort;
import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverSidedTileEntity;
import li.cil.oc.integration.ManagedTileEntityEnvironment;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

public final class DriverReactorRedstonePort extends DriverSidedTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return TileEntityReactorRedstonePort.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final BlockPos pos, final EnumFacing side) {
        return new Environment((TileEntityReactorRedstonePort) world.getTileEntity(pos));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<TileEntityReactorRedstonePort> implements NamedBlock {
        public Environment(final TileEntityReactorRedstonePort tileEntity) {
            super(tileEntity, "reactor_redstone_port");
        }

        @Override
        public String preferredName() {
            return "reactor_redstone_port";
        }

        @Override
        public int priority() {
            return 0;
        }

        private IReactor getReactor() {
            final FluidReactorLookup lookup = tileEntity.getComponent(FluidReactorLookup.class);
            if (lookup != null) {
                return lookup.getReactor();
            } else {
                return null;
            }
        }

        @Callback(doc = "function():number -- Get the reactor's heat.")
        public Object[] getHeat(final Context context, final Arguments args) {
            final IReactor reactor = getReactor();
            if (reactor != null) {
                return new Object[]{reactor.getHeat()};
            } else {
                return new Object[]{0};
            }
        }

        @Callback(doc = "function():number -- Get the reactor's maximum heat before exploding.")
        public Object[] getMaxHeat(final Context context, final Arguments args) {
            final IReactor reactor = getReactor();
            if (reactor != null) {
                return new Object[]{reactor.getMaxHeat()};
            } else {
                return new Object[]{0};
            }
        }

        @Callback(doc = "function():number -- Get the reactor's energy output. Not multiplied with the base EU/t value.")
        public Object[] getReactorEnergyOutput(final Context context, final Arguments args) {
            final IReactor reactor = getReactor();
            if (reactor != null) {
                return new Object[]{reactor.getReactorEnergyOutput()};
            } else {
                return new Object[]{0};
            }
        }

        @Callback(doc = "function():number -- Get the reactor's base EU/t value.")
        public Object[] getReactorEUOutput(final Context context, final Arguments args) {
            final IReactor reactor = getReactor();
            if (reactor != null) {
                return new Object[]{getReactor().getReactorEUEnergyOutput()};
            } else {
                return new Object[]{false};
            }
        }

        @Callback(doc = "function():boolean -- Get whether the reactor is active and supposed to produce energy.")
        public Object[] producesEnergy(final Context context, final Arguments args) {
            final IReactor reactor = getReactor();
            if (reactor != null) {
                return new Object[]{reactor.produceEnergy()};
            } else {
                return new Object[]{false};
            }
        }
    }
}
