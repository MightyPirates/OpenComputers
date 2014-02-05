package appeng.api.me.util;

import java.util.List;

import appeng.api.IItemList;
import appeng.api.me.tiles.ITilePushable;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * this class can be implemented if you wish to implement a TileCraftingProvider.
 */
public interface ICraftingPattern
{
	/**
	 * returns the outcome, MUST have a value.
	 * @return
	 */
    ItemStack getOutput();
    
    /**
     * Returns a condensed list of requirements.
     * 
     * Example: sticks, will return a single stack of 2, rather then two stacks of 1.
     * The same Item will not show more than one stack.
     */
    public List<ItemStack> getRequirements();
    
    /**
     * returns a list of all providers, called by AE
     * @return
     */
	List<ITileCraftingProvider> getProviders();
	
	/**
	 * adds a provider to the pattern, called by AE
	 * @param a
	 */
	void addProviders(ITileCraftingProvider a);
	
	/**
	 * Compare to Patterns, make sure you check your class types.
	 * @param obj
	 * @return
	 */
	@Override
	public boolean equals(Object obj);
	
}

