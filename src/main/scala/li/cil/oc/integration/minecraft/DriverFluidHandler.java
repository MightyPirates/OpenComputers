package li.cil.oc.integration.minecraft;

import li.cil.oc.api.driver.DriverBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentItem;
import li.cil.oc.integration.ManagedTileEntityEnvironment;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;

public final class DriverFluidHandler implements DriverBlock {
    @Override
    public boolean worksWith(final World world, final BlockPos pos, final EnumFacing side) {
        final TileEntity tileEntity = world.getTileEntity(pos);
        if (tileEntity == null) {
            return false;
        }
        return tileEntity.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, side) &&
                tileEntity.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, side) != null;
    }

    @Override
    public EnvironmentItem createEnvironment(final World world, final BlockPos pos, final EnumFacing side) {
        return new Environment(world.getTileEntity(pos).getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, side));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<IFluidHandler> {
        public Environment(final IFluidHandler tileEntity) {
            super(tileEntity, "fluid_handler");
        }

        @Callback(doc = "function():table -- Get some information about the tank accessible from the specified side.")
        public Object[] getTankInfo(final Context context, final Arguments args) {
            return tileEntity.getTankProperties();
        }
    }
}
