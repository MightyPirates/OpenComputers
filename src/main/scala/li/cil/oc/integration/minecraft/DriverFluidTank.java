package li.cil.oc.integration.minecraft;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.NodeContainerItem;
import li.cil.oc.api.prefab.driver.AbstractDriverTileEntity;
import li.cil.oc.integration.ManagedTileEntityNodeContainer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraftforge.fluids.IFluidTank;

public final class DriverFluidTank extends AbstractDriverTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return IFluidTank.class;
    }

    @Override
    public NodeContainerItem createEnvironment(final World world, final BlockPos pos, final EnumFacing side) {
        return new NodeContainer((IFluidTank) world.getTileEntity(pos));
    }

    public static final class NodeContainer extends ManagedTileEntityNodeContainer<IFluidTank> {
        public NodeContainer(final IFluidTank tileEntity) {
            super(tileEntity, "fluid_tank");
        }

        @Callback(doc = "function():table -- Get some information about this tank.")
        public Object[] getInfo(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getInfo()};
        }
    }
}