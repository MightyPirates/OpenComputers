package appeng.api;

import java.lang.reflect.InvocationTargetException;

import cpw.mods.fml.relauncher.ReflectionHelper;
import net.minecraft.item.ItemStack;
import appeng.api.me.util.ICraftingPattern;
import appeng.api.me.util.ICraftingPatternMAC;
import appeng.api.me.util.IMEInventory;
import appeng.api.me.util.IMEInventoryHandler;
import appeng.api.me.util.IMEInventoryUtil;

/**
 * Returns useful stuff of various sorts to access internal features and stuff, the meat of the important stuff is accessed here...
 * 
 * Available IMCs:
 * "AppliedEnergistics", "add-grindable", "itemid,meta,itemid,meta,rotations" - AE uses the standard 8 for ores, 4 for ingots.
 * "AppliedEnergistics", "blacklist-cell" "itemid[,meta]"
 * "AppliedEnergistics", "blacklist-transitionplane" "itemid[,meta]"
 * "AppliedEnergistics", "whitelist-transitionplane" "itemid[,meta]"
 * 
 */
public class Util
{
	static private IAppEngApi api = null;
	
	/**
	 * All future API calls should be made via this method.
	 * @return
	 */
	public static IAppEngApi getAppEngApi()
	{
		try {
			Class c = ReflectionHelper.getClass( Util.class.getClassLoader(), "appeng.common.AppEngApi" );
			api = (IAppEngApi) c.getMethod( "getInstance" ).invoke( c );
		} catch ( Throwable e) {
			return null;
		}
		return api;
	}
	
    /**
     * returns the wireless terminal registry.
     * @return
     */
    public static IWirelessTermRegistery getWirelessTermRegistery()
    {
    	if ( api == null ) return null;
    	return api.getWirelessRegistry();
    }
    
    /**
     * Find an object by its serial.
     * @param ser
     * @return LocatedObject or null
     */
    public static Object getLocateableBySerial( long ser )
    {
    	if ( api == null ) return null;
    	return api.getLocateableBySerial( ser );
    }
    
    /**
     * Creates a new AEItemstack.
     * @param is
     * @return newly generated AE Itemstack
     */
    public static IAEItemStack createItemStack( ItemStack is )
    {
    	if ( api == null ) return null;
    	return api.createItemStack( is );
    }
    
    /**
     * Simple Wrapper of the insertion process..
     * @param inv
     * @param is
     * @return ItemsNotInserted or null
     */
    public static ItemStack addItemsToInv( IMEInventory inv, ItemStack is )
    {
    	if ( api == null ) return null;
    	return api.addItemsToInv( inv, is );
    }
    
    /**
     * Simple Wrapper of the extraction process
     * @param inv
     * @param is
     * @return ItemsExtracted or null
     */
    public static ItemStack extractItemsFromInv( IMEInventory inv, ItemStack is )
    {
    	if ( api == null ) return null;
    	return api.extractItems( inv, is );
    }
    
    /**
     * Create a new Blank ItemList
     * @return new itemlist.
     */
    public static IItemList createItemList()
    {
    	if ( api == null ) return null;
    	return api.createItemList();
    }
    
    /**
     * creates a new IMEInventoryUtil, only useful if you want to use the fancy get items by recipe functionaility.
     * @param ime
     * @return created InvUtil
     */
    public static IMEInventoryUtil getIMEInventoryUtil( IMEInventory ime )
    {
    	if ( api == null ) return null;
    	return api.getIMEInventoryUtil( ime );
    }
    
    /**
     * Gets the instance of the special comparison registry ( Bees / Trees ) that sort of stuff
     * @return specialComparisonRegistry
     */
    public static ISpecialComparisonRegistry getSpecialComparisonRegistry()
    {
    	if ( api == null ) return null;
    	return api.getSpecialComparsonRegistry();
    }
    
    /**
     * Gets the instance of the external storage registry - Storage Bus
     * @return externStorgeRegitry
     */
    public static IExternalStorageRegistry getExternalStorageRegistry()
    {
    	if ( api == null ) return null;
    	return api.getExternalStorageRegistry();
    }
    
    /**
     * Gets the instance of the Cell Registry
     * @return returns the cell registry
     */
    public static ICellRegistry getCellRegistry()
    {
    	if ( api == null ) return null;
    	return api.getCellRegistry();
    }
    
    /**
     * Gets instance for the grinder recipe manager.
     * @return the grinder manager instance.
     */
    public static IGrinderRecipeManager getGrinderRecipeManage()
    {
    	if ( api == null ) return null;
    	return api.getGrinderRecipeManage();
    }

    /** Is it a Blank Pattern? */
    public static Boolean isBlankPattern(ItemStack i)
    {
    	if ( api == null ) return null;
    	return api.isBlankPattern( i );
    }
    
    /** Is it an IAssemblerPattern? */
    public static Boolean isAssemblerPattern(ItemStack i)
    {
    	if ( api == null ) return null;
    	return api.isAssemblerPattern( i );
    }
    
    /** Gets the IAssemblerPattern of the Assembly Pattern. */
    public static ICraftingPatternMAC getAssemblerPattern(ItemStack i)
    {
    	if ( api == null ) return null;
    	return api.getAssemblerPattern( i );
    }
    
    /** Is it a IStorageCell, this will only return true for IStoreCells and not custom cells, you should probobly not use it unless you have a specific case. */
    public static Boolean isBasicCell(ItemStack i)
    {
    	if ( api == null ) return null;
    	return api.isBasicCell( i );
    }
    
    /** 
     * if the item is a ME Compatible Storage Cell of any type.
     * @param i
     * @return true, if it is a storage call.
     */
    public static Boolean isCell(ItemStack i)
    {
    	if ( api == null ) return null;
    	return getCellRegistry().isCellHandled( i );
    }
    
    /**
     * Gets the Interface to insert/extract from the Storage Cell for the item.
     * @param i
     * @return newly procured cell handler.
     */
    public static IMEInventoryHandler getCell(ItemStack i)
    {
    	if ( api == null ) return null;
    	return getCellRegistry().getHandlerForCell( i );
    }
    
    /**
     * Lets you access internal storage of IStorageCell's
     * @param i
     * @return only works with Basic Cells, not custom ones, suggested not to use.
     */
    public static IMEInventory getBasicCell(ItemStack i)
    {
    	if ( api == null ) return null;
    	return api.getBasicCell( i );
    }
    
    /**
     * Lets you blast list a specific item from being stored in basic cells, this works on any mod cells that use IStorageCell as well.
     * @param ItemID
     * @param Meta
     */
    public static void addBasicBlackList( int ItemID, int Meta )
    {
    	if ( api == null ) return;
    	api.addBasicBlackList( ItemID, Meta );
    }
    
}
