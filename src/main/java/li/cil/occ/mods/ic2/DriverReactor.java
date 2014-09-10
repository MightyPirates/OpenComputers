package li.cil.occ.mods.ic2;


import ic2.api.reactor.IReactor;
import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.network.Arguments;
import li.cil.oc.api.network.Callback;
import li.cil.oc.api.network.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverTileEntity;
import li.cil.occ.mods.ManagedTileEntityEnvironment;
import net.minecraft.world.World;

public final class DriverReactor extends DriverTileEntity implements NamedBlock {
    @Override
    public Class<?> getTileEntityClass() {
        return IReactor.class;
    }

    @Override
    public String preferredName() {
        return "reactor";
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z) {
        return new Environment((IReactor) world.getTileEntity(x, y, z));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<IReactor> {
        public Environment(final IReactor tileEntity) {
            super(tileEntity, "reactor");
        }

        @Callback(doc = "Get the reactor's heat.")
        public Object[] getHeat(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getHeat()};
        }

        @Callback(doc = "Get the reactor's maximum heat before exploding.")
        public Object[] getMaxHeat(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getMaxHeat()};
        }

        @Callback(doc = "Get the reactor's energy output. Not multiplied with the base EU/t value.")
        public Object[] getReactorEnergyOutput(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getReactorEnergyOutput()};
        }

        @Callback(doc = "Get the reactor's base EU/t value.")
        public Object[] getReactorEUOutput(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getReactorEUEnergyOutput()};
        }

        @Callback(doc = "Get whether the reactor is active and supposed to produce energy.")
        public Object[] producesEnergy(final Context context, final Arguments args) {
            return new Object[]{tileEntity.produceEnergy()};
        }
    }
}
