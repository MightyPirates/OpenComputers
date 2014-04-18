package appeng.api.networking;

import java.util.HashMap;

/**
 * A registry of grid caches to extend grid functionality.
 */
public interface IGridCacheRegistry
{

	/**
	 * Register a new grid cache for use during operation, must be called during the loading phase.
	 * 
	 * @param cl
	 * @return cache id ( can be used later to ask the grid for you cache. )
	 */
	void registerGridCache(Class<? extends IGridCache> iface, Class<? extends IGridCache> implementation);

	/**
	 * requests a new instance of a grid cache for use, used internally
	 * 
	 * @param grid
	 * 
	 * @return a new HashMap of IGridCaches from the registry, called from IGrid when constructing a new grid.
	 */
	HashMap<Class<? extends IGridCache>, IGridCache> createCacheInstance(IGrid grid);

}
