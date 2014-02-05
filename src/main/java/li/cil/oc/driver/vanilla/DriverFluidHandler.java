package li.cil.oc.driver.vanilla;

import li.cil.oc.api.network.Arguments;
import li.cil.oc.api.network.Callback;
import li.cil.oc.api.network.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.driver.ManagedTileEntityEnvironment;
import li.cil.oc.driver.TileEntityDriver;
import li.cil.oc.util.TypeConversion;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeDirection;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;

import java.util.Map;

public final class DriverFluidHandler extends TileEntityDriver {
    @Override
    public Class<?> getFilterClass() {
        return IFluidHandler.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z) {
        return new Environment((IFluidHandler) world.getBlockTileEntity(x, y, z));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<IFluidHandler> {
        public Environment(final IFluidHandler tileEntity) {
            super(tileEntity, "fluid_handler");
        }

        @Callback
        public Object[] getTankInfo(final Context context, final Arguments args) {
            ForgeDirection side = args.count() > 0 ? ForgeDirection.getOrientation(args.checkInteger(0)) : ForgeDirection.UNKNOWN;
            FluidTankInfo[] info = tileEntity.getTankInfo(side);
            Map[] result = new Map[info.length];
            for (int i = 0; i < info.length; ++i) {
                result[i] = TypeConversion.toMap(info[i]);
            }
            return new Object[]{result};
        }
    }
}
