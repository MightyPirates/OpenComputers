package appeng.api;

import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.ForgeDirection;

public interface IExternalStorageRegistry {
	
	/**
	 * A registry for StorageBus interactions
	 * @param ei
	 */
	void addExternalStorageInterface( IExternalStorageHandler ei );
	
	/**
	 * returns the handler for a given tile / forge direction.
	 * @param te
	 * @param opposite
	 * @return
	 */
	IExternalStorageHandler getHandler(TileEntity te, ForgeDirection opposite);
	
}
