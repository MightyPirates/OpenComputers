package li.cil.oc.integration.ic2;

import ic2.api.reactor.IReactor;
import ic2.api.reactor.IReactorChamber;
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

public final class DriverReactorChamber extends DriverSidedTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return IReactorChamber.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final BlockPos pos, final EnumFacing side) {
        return new Environment((IReactorChamber) world.getTileEntity(pos));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<IReactorChamber> implements NamedBlock {
        public Environment(final IReactorChamber tileEntity) {
            super(tileEntity, "reactor_chamber");
        }

        @Override
        public String preferredName() {
            return "reactor_chamber";
        }

        @Override
        public int priority() {
            return 0;
        }

        @Callback(doc = "function():number -- Get the reactor's heat.")
        public Object[] getHeat(final Context context, final Arguments args) {
            final IReactor reactor = tileEntity.getReactorInstance();
            if (reactor != null) {
                return new Object[]{reactor.getHeat()};
            } else {
                return new Object[]{0};
            }
        }

        @Callback(doc = "function():number -- Get the reactor's maximum heat before exploding.")
        public Object[] getMaxHeat(final Context context, final Arguments args) {
            final IReactor reactor = tileEntity.getReactorInstance();
            if (reactor != null) {
                return new Object[]{tileEntity.getReactorInstance().getMaxHeat()};
            } else {
                return new Object[]{0};
            }
        }

        @Callback(doc = "function():number -- Get the reactor's energy output. Not multiplied with the base EU/t value.")
        public Object[] getReactorEnergyOutput(final Context context, final Arguments args) {
            final IReactor reactor = tileEntity.getReactorInstance();
            if (reactor != null) {
                return new Object[]{tileEntity.getReactorInstance().getReactorEnergyOutput()};
            } else {
                return new Object[]{0};
            }
        }

        @Callback(doc = "function():number -- Get the reactor's base EU/t value.")
        public Object[] getReactorEUOutput(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getReactorInstance().getReactorEUEnergyOutput()};
        }

        @Callback(doc = "function():boolean -- Get whether the reactor is active and supposed to produce energy.")
        public Object[] producesEnergy(final Context context, final Arguments args) {
            final IReactor reactor = tileEntity.getReactorInstance();
            if (reactor != null) {
                return new Object[]{tileEntity.getReactorInstance().produceEnergy()};
            } else {
                return new Object[]{false};
            }
        }
    }
}
