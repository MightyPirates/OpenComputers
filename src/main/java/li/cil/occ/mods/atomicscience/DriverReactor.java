package li.cil.occ.mods.atomicscience;

import atomicscience.api.IReactor;
import li.cil.oc.api.network.Arguments;
import li.cil.oc.api.network.Callback;
import li.cil.oc.api.network.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverTileEntity;
import li.cil.occ.mods.ManagedTileEntityEnvironment;
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

        @Callback(doc = "function():boolean --  Returns whether the reactor is overtoxic.")
        public Object[] isOverToxic(final Context context, final Arguments args) {
            return new Object[]{tileEntity.isOverToxic()};
        }
    }
}
