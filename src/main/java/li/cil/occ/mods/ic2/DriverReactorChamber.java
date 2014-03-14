package li.cil.occ.mods.ic2;

import ic2.api.reactor.IReactor;
import ic2.api.reactor.IReactorChamber;
import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.network.Arguments;
import li.cil.oc.api.network.Callback;
import li.cil.oc.api.network.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverTileEntity;
import li.cil.occ.mods.ManagedTileEntityEnvironment;
import net.minecraft.world.World;

public final class DriverReactorChamber extends DriverTileEntity implements NamedBlock {
    @Override
    public Class<?> getTileEntityClass() {
        return IReactorChamber.class;
    }

    @Override
    public String preferredName() {
        return "reactor_chamber";
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z) {
        return new Environment((IReactorChamber) world.getTileEntity(x, y, z));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<IReactorChamber> {
        public Environment(final IReactorChamber tileEntity) {
            super(tileEntity, "reactor_chamber");
        }

        @Callback
        public Object[] getHeat(final Context context, final Arguments args) {
            final IReactor reactor = tileEntity.getReactor();
            if (reactor != null) {
                return new Object[]{reactor.getHeat()};
            } else {
                return new Object[]{0};
            }
        }

        @Callback
        public Object[] getMaxHeat(final Context context, final Arguments args) {
            final IReactor reactor = tileEntity.getReactor();
            if (reactor != null) {
                return new Object[]{tileEntity.getReactor().getMaxHeat()};
            } else {
                return new Object[]{0};
            }
        }

        @Callback
        public Object[] getReactorEnergyOutput(final Context context, final Arguments args) {
            final IReactor reactor = tileEntity.getReactor();
            if (reactor != null) {
                return new Object[]{tileEntity.getReactor().getReactorEnergyOutput()};
            } else {
                return new Object[]{0};
            }
        }

        @Callback
        public Object[] producesEnergy(final Context context, final Arguments args) {
            final IReactor reactor = tileEntity.getReactor();
            if (reactor != null) {
                return new Object[]{tileEntity.getReactor().produceEnergy()};
            } else {
                return new Object[]{false};
            }
        }
    }
}
