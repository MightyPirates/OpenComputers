package li.cil.occ.handler.ic2;

import ic2.api.reactor.IReactor;
import li.cil.oc.api.network.Arguments;
import li.cil.oc.api.network.Callback;
import li.cil.oc.api.network.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverTileEntity;
import li.cil.occ.handler.ManagedTileEntityEnvironment;
import net.minecraft.world.World;

public final class DriverReactor extends DriverTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return IReactor.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z) {
        return new Environment((IReactor) world.getBlockTileEntity(x, y, z));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<IReactor> {
        public Environment(final IReactor tileEntity) {
            super(tileEntity, "reactor");
        }

        @Callback
        public Object[] getHeat(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getHeat()};
        }

        @Callback
        public Object[] getMaxHeat(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getMaxHeat()};
        }

        @Callback
        public Object[] getReactorEnergyOutput(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getReactorEnergyOutput()};
        }

        @Callback
        public Object[] producesEnergy(final Context context, final Arguments args) {
            return new Object[]{tileEntity.produceEnergy()};
        }
    }
}
