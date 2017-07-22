package li.cil.oc.integration.vanilla;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverSidedTileEntity;
import li.cil.oc.integration.ManagedTileEntityEnvironment;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.IFluidTank;

public final class DriverFluidTank extends DriverSidedTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return IFluidTank.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z, final ForgeDirection side) {
        return new Environment((IFluidTank) world.getTileEntity(x, y, z));
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