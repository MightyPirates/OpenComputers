package appeng.api.me.util;

import net.minecraft.nbt.NBTTagCompound;

/**
 * 
 * Allows you to cache information on the grid, for usage later, its up to you how you want to deal with saving/loading/resetting.
 * 
 * Any Class that implements this, should have a default constructor that takes no arguments.
 * 
 * Register this with Util.getAppEngApi().registerGridCache( MyGridCache.class );
 * 
 */
public interface IGridCache {
	
	/**
	 * Called each time a network is reset ( changed ), the grid will persist your classes instance, but call this method.
	 * @param grid
	 */
	void reset( IGridInterface grid );
	
	/**
	 * Called each tick for the controller, allows you to have active network wide behaviors.
	 * @param grid
	 */
	void onUpdateTick( IGridInterface grid );
	
	/**
	 * used to uniquely identify your cache when saving/loading
	 * @return
	 */
	String getCacheName();
	
	/**
	 * Save your caches state if necessary, if no saving is required, return null.
	 * @return
	 */
	NBTTagCompound savetoNBTData();
	
	/**
	 * Load save cache data, this is called when a controller is loaded with previously saved data if there was no data, data will be an empty tag.
	 * @param data
	 */
	void loadfromNBTData( NBTTagCompound data );
	
}
