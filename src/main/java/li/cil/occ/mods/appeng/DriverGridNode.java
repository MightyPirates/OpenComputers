package li.cil.occ.mods.appeng;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.energy.IEnergyGrid;
import li.cil.oc.api.network.Arguments;
import li.cil.oc.api.network.Callback;
import li.cil.oc.api.network.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverTileEntity;
import li.cil.occ.mods.ManagedTileEntityEnvironment;
import net.minecraft.world.World;

public final class DriverGridNode extends DriverTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return IGridNode.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z) {
        return new Environment((IGridNode) world.getTileEntity(x, y, z));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<IGridNode> {
        public Environment(final IGridNode tileEntity) {
            super(tileEntity, "me_node");
        }

        @Callback(doc = "function():number -- Returns the average of energy usage over the last 10 ticks.")
        public Object[] getAvgPowerUsage(final Context context, final Arguments args) {
            final IGrid grid = tileEntity.getGrid();
            return grid != null
                    ? new Object[]{((IEnergyGrid) grid.getCache(IEnergyGrid.class)).getAvgPowerUsage()}
                    : new Object[]{null, "no grid"};
        }

        @Callback(doc = "function():number -- Returns the total amount of power available.")
        public Object[] getStoredPower(final Context context, final Arguments args) {
            final IGrid grid = tileEntity.getGrid();
            return grid != null
                    ? new Object[]{((IEnergyGrid) grid.getCache(IEnergyGrid.class)).getStoredPower()}
                    : new Object[]{null, "no grid"};
        }

        @Callback(doc = "function():boolean -- Returns whether the network is powered.")
        public Object[] isNetworkPowered(final Context context, final Arguments args) {
            final IGrid grid = tileEntity.getGrid();
            return grid != null
                    ? new Object[]{((IEnergyGrid) grid.getCache(IEnergyGrid.class)).isNetworkPowered()}
                    : new Object[]{null, "no grid"};
        }
    }
}
