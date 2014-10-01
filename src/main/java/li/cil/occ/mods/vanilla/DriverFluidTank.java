package li.cil.occ.mods.vanilla;

import li.cil.oc.api.network.Arguments;
import li.cil.oc.api.network.Callback;
import li.cil.oc.api.network.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverTileEntity;
import li.cil.occ.mods.ManagedTileEntityEnvironment;
import net.minecraft.world.World;
import net.minecraftforge.fluids.IFluidTank;

public final class DriverFluidTank extends DriverTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return IFluidTank.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z) {
        return new Environment((IFluidTank) world.getBlockTileEntity(x, y, z));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<IFluidTank> {
        public Environment(final IFluidTank tileEntity) {
            super(tileEntity, "fluid_tank");
        }

        @Callback(doc = "function():table -- Get some information about this tank.")
        public Object[] getInfo(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getInfo()};
        }
    }
}