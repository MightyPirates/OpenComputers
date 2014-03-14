package appeng.api.me.util;

import appeng.api.IAEItemStack;
import appeng.api.IItemList;

/**
 * Lets you access Internal Cell Inventories.
 */
public interface IMEInventory
{
    /**
     * The number of different item types stored in inventory.
     */
    public long storedItemTypes();
    
    /**
     * The number of stored items total, regardless of type.
     */
    public long storedItemCount();
    
    /**
     * The estimated number of additional items this inventory can hold, regardless of type.
     */
    public long remainingItemCount();
    
    /**
     * The estimated number of additional types the inventory could hold.
     */
    public long remainingItemTypes();

    /**
     * True or False if this item is inside this inventory.
     */
    public boolean containsItemType(IAEItemStack i);
    
    /**
     * The total number of types holdable in this inventory.
     */
    public long getTotalItemTypes();
    
    /**
     *  Returns how many of this item are in the inventory, regardless of a how many stacks / cells or anything else.
     */
    public long countOfItemType(IAEItemStack i);
    
    /**
     *  Adds input, to the inventory, and returns items not added, complete failure yields a copy of the stack that was passed.
     */
    public IAEItemStack addItems( IAEItemStack input );
    
    /**
     * Attempts to extract the requested item, in the count specified by the request, returns items extracted, complete failure yields NULL.
     */
    public IAEItemStack extractItems( IAEItemStack request );
    
    // DO NOT USE ItemStack.split, these are for information purpose ONLY!
    // I you wan to remove items from the cell, use extractItemss
    
    /**
     * Returns a list of all available items, with stackSize set to the real amount, without stack limits.
     */
    public IItemList getAvailableItems( IItemList out );
    
    /**
     * Returns a list of all available items, with stackSize set to the real amount, without stack limits.
     */
    public IItemList getAvailableItems();
    
    /**
     * Identical to addItems(...) but it don't change anything, its just a simulation.
     * this is used when dealing with pipes/tubes, and routing.
     */
    public IAEItemStack calculateItemAddition(IAEItemStack stack);
    
	// calculates available space for a specific item.
	public long getAvailableSpaceByItem( IAEItemStack i, long maxNeeded );
}
