package li.cil.oc.driver.atomicscience;

import atomicscience.api.IReactor;
import buildcraft.core.IMachine;
import li.cil.oc.api.network.Arguments;
import li.cil.oc.api.network.Callback;
import li.cil.oc.api.network.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverTileEntity;
import li.cil.oc.driver.ManagedTileEntityEnvironment;
import net.minecraft.world.World;


public class DriverReactor extends DriverTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return IReactor.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(World world, int x, int y, int z) {
        return new Environment((IReactor) world.getBlockTileEntity(x, y, z));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<IReactor> {
        public Environment(final IReactor tileEntity) {
            super(tileEntity, "reactor");
        }

        @Callback
        public Object[] isOverToxic(final Context context, final Arguments args) {
            return new Object[]{tileEntity.isOverToxic()};
        }
    }
}
