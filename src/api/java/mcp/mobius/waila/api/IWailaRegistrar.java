package mcp.mobius.waila.api;

public interface IWailaRegistrar {
	/* Add a config option in the section modname with displayed text configtext and access key keyname */
	public void addConfig(String modname, String keyname, String configtext);
	public void addConfigRemote(String modname, String keyname, String configtext);	
	public void addConfig(String modname, String keyname);
	public void addConfigRemote(String modname, String keyname);	
	
	/* Register a IWailaDataProvider for the given blockID, either for the Head section or the Body section */
	@Deprecated
	public void registerHeadProvider (IWailaDataProvider dataProvider, int blockID);
	@Deprecated
	public void registerBodyProvider (IWailaDataProvider dataProvider, int blockID);
	@Deprecated
	public void registerTailProvider (IWailaDataProvider dataProvider, int blockID);	
	
	/* Register a stack overrider for the given blockID */
	@Deprecated
	public void registerStackProvider(IWailaDataProvider dataProvider, int blockID);
	public void registerStackProvider(IWailaDataProvider dataProvider, Class block);	
	
	/* Same thing, but works on a class hierarchy instead */
	public void registerHeadProvider (IWailaDataProvider dataProvider, Class block);
	public void registerBodyProvider (IWailaDataProvider dataProvider, Class block);
	public void registerTailProvider (IWailaDataProvider dataProvider, Class block);	

	/* Entity text registration methods */
	public void registerHeadProvider     (IWailaEntityProvider dataProvider, Class entity);
	public void registerBodyProvider     (IWailaEntityProvider dataProvider, Class entity);
	public void registerTailProvider     (IWailaEntityProvider dataProvider, Class entity);
	public void registerOverrideEntityProvider (IWailaEntityProvider dataProvider, Class entity);
	
	/* FMP Providers */
	public void registerHeadProvider(IWailaFMPProvider dataProvider, String name);
	public void registerBodyProvider(IWailaFMPProvider dataProvider, String name);
	public void registerTailProvider(IWailaFMPProvider dataProvider, String name);
	
	/* The block decorators */
	@Deprecated
	public void registerDecorator (IWailaBlockDecorator decorator, int blockID);
	public void registerDecorator (IWailaBlockDecorator decorator, Class block);
	public void registerDecorator (IWailaFMPDecorator decorator,   String name);	
	
	/* Selective NBT key syncing. Will register a key to sync over the network for the given class (block, te or ent).  
	 * Accept * as a ending wildcard 
	 * registerNBTKey("bob.*", MyBlock.class)
	 * registerNBTKey("data.life", MyEntity.class) 
	 * registerNBTKey("*", MyTileEntity.class) will reproduce the full tag syncing from 1.4.5 
	 * */
	public void registerSyncedNBTKey(String key, Class target);

	/* UNUSED FOR NOW (Will be used for the ingame wiki */
	public void registerDocTextFile  (String filename);
	public void registerShortDataProvider (IWailaSummaryProvider dataProvider, Class item);
}
