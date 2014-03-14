package appeng.api;

import appeng.api.me.util.IGridCache;

/**
 * A registry of grid caches to extend grid functionality.
 */
public interface IGridCacheRegistry {
	
	/**
	 * Register a new grid cache for use during operation, must be called during the loading phase.
	 * @param cl
	 * @return cache id ( can be used later to ask the grid for you cache. )
	 */
	int registerGridCache( Class<? extends IGridCache> cl );
	
	/**
	 * requests a new instance of a grid cache for use, used internally
	 * @param id
	 * @return
	 */
	IGridCache[] createCacheInstance();
	
}
