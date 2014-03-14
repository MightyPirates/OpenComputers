package appeng.api;

import appeng.api.me.items.IMemoryCard;
import appeng.api.me.items.ISpatialStorage;
import appeng.api.me.tiles.IColoredMETile;
import appeng.api.me.util.ICraftingPattern;
import appeng.api.me.util.ICraftingPatternMAC;
import appeng.api.me.util.IMEInventory;
import appeng.api.me.util.IMEInventoryUtil;
import appeng.api.movable.IMovableRegistry;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public interface IAppEngApi {

	/**
	 * Attempts to paint the block at the specified coords, the specified color, colors are based off of IColoredMETile.Colors
	 * @param world
	 * @param x
	 * @param y
	 * @param z
	 * @param color
	 * @return if the block was painted, or not you can use this to damage your item if you want.
	 */
	boolean paintBlock(World world, int x, int y, int z, int color);
	
	/**
	 * Use the movable registry to white list your tiles.
	 * @param c
	 */
	IMovableRegistry getMovableRegistry();
	
	/**
	 * Add new Grid Caches for use during run time, only use during loading phase.
	 * @return 
	 */
	IGridCacheRegistry getGridCacheRegistry();
	
	/**
	 * Add additional storage bus handlers to improve interplay with mod blocks that contains special inventories that function unlike vanilla chests.
	 * AE uses this internally for barrels, dsu's, quantum chests, AE Networks and more.	 * 
	 * @return IExternalStorageRegistry
	 */
	IExternalStorageRegistry getExternalStorageRegistry();

	/**
	 * Add additional special comparison functionality, AE Uses this internally for Bees.
	 * @return ISpecialComparisonRegistry
	 */
	ISpecialComparisonRegistry getSpecialComparsonRegistry();
	
	/**
	 * gives you access to extractItemsByRecipe, other then this theres not much use for this.	
	 * @param mei
	 * @return
	 */
	IMEInventoryUtil getIMEInventoryUtil(IMEInventory mei);

	/**
	 * Lets you register your items as wireless terminals
	 * @return
	 */
	IWirelessTermRegistery getWirelessRegistry();

	/**
	 * return an locateable object by its serial
	 * used internally by AE for QNBs / Network Controllers
	 * @param serial
	 * @return
	 */
	Object getLocateableBySerial(Long serial);
	
	/**
	 * creates a new instance of IAEItemStack from a MC ItemStack
	 * @param is
	 * @return
	 */
	IAEItemStack createItemStack(ItemStack is);

	/**
	 * Creates a new instance of IItemList
	 * @return
	 */
	IItemList createItemList();

	/**
	 * Allows you to register new cell types, these will function in drives
	 * @return
	 */
	ICellRegistry getCellRegistry();

	/**
	 * Manage grinder recipes via API
	 * @return
	 */
	IGrinderRecipeManager getGrinderRecipeManage();

	/**
	 * get access to the locatable registry
	 * @return
	 */
	ILocateableRegistry getLocateableRegistry();

	/**
	 * a simple wrapper around the AEItemStack version on IMEInventory
	 * @param inv
	 * @param is
	 * @return
	 */
	ItemStack addItemsToInv(IMEInventory inv, ItemStack is);

	/**
	 * a simple wrapper around the AEItemStack version on IMEInventory
	 * @param inv
	 * @param is
	 * @return
	 */
	ItemStack extractItems(IMEInventory inv, ItemStack is);

	/**
	 * Blacklist this item from vanilla AE Terminals / IStorageCell
	 * @param itemID
	 * @param meta - if you use OreDictionary.WILDCARD_VALUE it will blacklist the entire itemID.
	 */
	void addBasicBlackList(int itemID, int meta);

	/**
	 * Acquire the inventory for a IStorageCell
	 * @param i
	 * @return
	 */
	IMEInventory getBasicCell(ItemStack i);

	/**
	 * checks if the item is a basic storage cell.
	 * @param i
	 * @return
	 */
	Boolean isBasicCell(ItemStack i);

	/**
	 * gives you access to assembler pattern inventories.
	 * @param i
	 * @return
	 */
	ICraftingPatternMAC getAssemblerPattern(ItemStack i);

	/**
	 * allows you to check if an item is an assembler pattern.
	 * @param i
	 * @return
	 */
	Boolean isAssemblerPattern(ItemStack i);

	/**
	 * allows you to check if a pattern is blank.
	 * @param i
	 * @return
	 */
	Boolean isBlankPattern(ItemStack i);

	/**
	 * gives you the ability to interface with AE Memory Cards.
	 * @return
	 */
	IMemoryCard getMemoryCardHandler();

	/**
	 * returns the ISpatialStorage for the ItemStack, if it can get from form its item ( or from AE )
	 * @param is
	 * @return
	 */
	ISpatialStorage getSpatialStorageForItem( ItemStack is );
	
}
