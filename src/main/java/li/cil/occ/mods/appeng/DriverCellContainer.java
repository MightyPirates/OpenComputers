package li.cil.occ.mods.appeng;

import appeng.api.storage.ICellContainer;
import appeng.api.storage.StorageChannel;
import li.cil.oc.api.network.Arguments;
import li.cil.oc.api.network.Callback;
import li.cil.oc.api.network.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverTileEntity;
import li.cil.occ.mods.ManagedTileEntityEnvironment;
import net.minecraft.world.World;

public class DriverCellContainer extends DriverTileEntity {
	@Override
	public Class<?> getTileEntityClass() {
		return ICellContainer.class;
	}

	@Override
	public ManagedEnvironment createEnvironment(World world, int x, int y, int z) {
		return new Evironment((ICellContainer)world.getTileEntity(x,y,z));
	}

	private class Evironment extends ManagedTileEntityEnvironment<ICellContainer> {
		public Evironment(ICellContainer tileEntity) {
			super(tileEntity, "cell_container");
		}

		@Callback(doc = "function():table -- Returns a table of the available item cells in this inventory.")
		public Object[] getItemCells(final Context context, final Arguments args){
			return tileEntity.getCellArray(StorageChannel.ITEMS).toArray();
		}

		@Callback(doc = "function():table -- Returns a table of the available fluid cells in this inventory.")
		public Object[] getFluidCells(final Context context, final Arguments args){
			return tileEntity.getCellArray(StorageChannel.FLUIDS).toArray();
		}
	}
}
