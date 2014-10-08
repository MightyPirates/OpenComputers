package li.cil.occ.mods.ic2;

import ic2.api.reactor.IReactor;
import ic2.api.reactor.IReactorChamber;
import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
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
    public int priority() {
        return 0;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z) {
        return new Environment((IReactorChamber) world.getTileEntity(x, y, z));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<IReactorChamber> {
        public Environment(final IReactorChamber tileEntity) {
            super(tileEntity, "reactor_chamber");
        }

        @Callback(doc = "function():number -- Get the reactor's heat.")
        public Object[] getHeat(final Context context, final Arguments args) {
            final IReactor reactor = tileEntity.getReactor();
            if (reactor != null) {
                return new Object[]{reactor.getHeat()};
            } else {
                return new Object[]{0};
            }
        }

        @Callback(doc = "function():number -- Get the reactor's maximum heat before exploding.")
        public Object[] getMaxHeat(final Context context, final Arguments args) {
            final IReactor reactor = tileEntity.getReactor();
            if (reactor != null) {
                return new Object[]{tileEntity.getReactor().getMaxHeat()};
            } else {
                return new Object[]{0};
            }
        }

        @Callback(doc = "function():number -- Get the reactor's energy output. Not multiplied with the base EU/t value.")
        public Object[] getReactorEnergyOutput(final Context context, final Arguments args) {
            final IReactor reactor = tileEntity.getReactor();
            if (reactor != null) {
                return new Object[]{tileEntity.getReactor().getReactorEnergyOutput()};
            } else {
                return new Object[]{0};
            }
        }

        @Callback(doc = "function():number -- Get the reactor's base EU/t value.")
        public Object[] getReactorEUOutput(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getReactor().getReactorEUEnergyOutput()};
        }

        @Callback(doc = "function():boolean -- Get whether the reactor is active and supposed to produce energy.")
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
