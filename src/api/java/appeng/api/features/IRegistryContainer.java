package appeng.api.features;

import appeng.api.movable.IMovableRegistry;
import appeng.api.networking.IGridCacheRegistry;
import appeng.api.storage.ICellRegistry;
import appeng.api.storage.IExternalStorageRegistry;

public interface IRegistryContainer
{

	/**
	 * Use the movable registry to white list your tiles.
	 */
	IMovableRegistry moveable();

	/**
	 * Add new Grid Caches for use during run time, only use during loading phase.
	 */
	IGridCacheRegistry gridCache();

	/**
	 * Add additional storage bus handlers to improve interplay with mod blocks that contains special inventories that
	 * function unlike vanilla chests. AE uses this internally for barrels, dsu's, quantum chests, AE Networks and more.
	 */
	IExternalStorageRegistry externalStorage();

	/**
	 * Add additional special comparison functionality, AE Uses this internally for Bees.
	 */
	ISpecialComparisonRegistry specialComparson();

	/**
	 * Lets you register your items as wireless terminals
	 */
	IWirelessTermRegistery wireless();

	/**
	 * Allows you to register new cell types, these will function in drives
	 */
	ICellRegistry cell();

	/**
	 * Manage grinder recipes via API
	 */
	IGrinderRegistry grinder();

	/**
	 * get access to the locatable registry
	 */
	ILocatableRegistry locateable();

	/**
	 * get access to the p2p tunnel registry.
	 */
	IP2PTunnelRegistry p2pTunnel();

	/**
	 * get access to the ammo registry.
	 */
	IMatterCannonAmmoRegistry matterCannon();

	/**
	 * get access to the player registry
	 */
	IPlayerRegistry players();
	
	/**
	 * get access to the ae2 recipe appeng.api
	 */
	IRecipeHandlerRegistry recipes();
	
}
