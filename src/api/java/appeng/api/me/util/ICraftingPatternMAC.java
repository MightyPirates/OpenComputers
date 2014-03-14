package appeng.api.me.util;

import java.util.List;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;
import appeng.api.IItemList;

/**
 * Interact with the internals of assembler patterns, get this via Util.getAssemblerPattern(...)
 * 
 * Do not implement this version, implement ICraftingPattern
 * 
 */
public interface ICraftingPatternMAC extends ICraftingPattern {

    /** 
     * Encode a pattern.
     * craftingMatrix - accepts a 3x3 grid of ItemStacks and Nulls.
     * output - accepts a single ItemStack, NEVER SEND NULL 
     */
    void encodePattern(ItemStack[] craftingMatrix, ItemStack output);
    
    /**
     * I have no idea what the World is for, its just part of IRecipe...
     */
    boolean isCraftable(World w);
    
    /** Returns true if there is a pattern encoded. */
    boolean isEncoded();
    	
	IAssemblerCluster getCluster();
	
    /** Returns a 3x3 matrix of nulls or ItemStacks, or null if it is not included. */
    ItemStack[] getCraftingMatrix();
    
    /** 
     * Same as getCraftingMatrix(), but gets a crafting inventory for real crafting.
     * 
     * Item pool is optional, null will work, but it won't be able to edit items... 
     */
    InventoryCrafting getCraftingInv( World w, IMEInventory itemPool, List<ItemStack> missing, List<ItemStack> used, IItemList all );

	/** returns the output of the pattern */
	ItemStack getRecipeOutput( InventoryCrafting ic, World w );
	
	/** returns the recipe for the patterns */
	IRecipe getMatchingRecipe( InventoryCrafting ic, World w );
	
}
