package li.cil.occ.mods.appeng;

import appeng.api.storage.ICellContainer;
import appeng.api.storage.StorageChannel;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverTileEntity;
import li.cil.occ.mods.ManagedTileEntityEnvironment;
import net.minecraft.world.World;

public final class DriverCellContainer extends DriverTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return ICellContainer.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z) {
        return new Evironment((ICellContainer) world.getTileEntity(x, y, z));
    }

    public static final class Evironment extends ManagedTileEntityEnvironment<ICellContainer> {
        public Evironment(final ICellContainer tileEntity) {
            super(tileEntity, "cell_container");
        }

        @Callback(doc = "function():table -- Returns a table of the available item cells in this inventory.")
        public Object[] getItemCells(final Context context, final Arguments args) {
            return tileEntity.getCellArray(StorageChannel.ITEMS).toArray();
        }

        @Callback(doc = "function():table -- Returns a table of the available fluid cells in this inventory.")
        public Object[] getFluidCells(final Context context, final Arguments args) {
            return tileEntity.getCellArray(StorageChannel.FLUIDS).toArray();
        }
    }
}
