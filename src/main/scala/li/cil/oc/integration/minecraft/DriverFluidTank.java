package li.cil.oc.integration.minecraft;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverSidedTileEntity;
import li.cil.oc.integration.ManagedTileEntityEnvironment;
import li.cil.oc.util.ExtendedArguments.TankProperties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.Direction;
import net.minecraft.world.World;
import net.minecraftforge.fluids.IFluidTank;

public final class DriverFluidTank extends DriverSidedTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return IFluidTank.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final BlockPos pos, final Direction side) {
        return new Environment((IFluidTank) world.getBlockEntity(pos));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<IFluidTank> {
        public Environment(final IFluidTank tileEntity) {
            super(tileEntity, "fluid_tank");
        }

        @Callback(doc = "function():table -- Get some information about this tank.")
        public Object[] getInfo(final Context context, final Arguments args) {
            return new Object[]{new TankProperties(tileEntity.getCapacity(), tileEntity.getFluid())};
        }
    }
}