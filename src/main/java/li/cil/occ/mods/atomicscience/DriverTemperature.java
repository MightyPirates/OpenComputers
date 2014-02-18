package li.cil.occ.mods.atomicscience;

import atomicscience.api.ITemperature;
import li.cil.oc.api.network.Arguments;
import li.cil.oc.api.network.Callback;
import li.cil.oc.api.network.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverTileEntity;
import li.cil.occ.mods.ManagedTileEntityEnvironment;
import net.minecraft.world.World;

public final class DriverTemperature extends DriverTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return ITemperature.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z) {
        return new Environment((ITemperature) world.getBlockTileEntity(x, y, z));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<ITemperature> {
        public Environment(final ITemperature tileEntity) {
            super(tileEntity, "temperature");
        }

        @Callback(doc = "function():number --  Gets the temperature of this block in kelvin.")
        public Object[] getTemperature(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getTemperature()};
        }
    }
}

