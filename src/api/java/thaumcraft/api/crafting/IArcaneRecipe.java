package thaumcraft.api.crafting;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import thaumcraft.api.aspects.AspectList;

public interface IArcaneRecipe
{
	
	
    /**
     * Used to check if a recipe matches current crafting inventory
     * @param player 
     */
    boolean matches(IInventory var1, World world, EntityPlayer player);

    /**
     * Returns an Item that is the result of this recipe
     */
    ItemStack getCraftingResult(IInventory var1);

    /**
     * Returns the size of the recipe area
     */
    int getRecipeSize();

    ItemStack getRecipeOutput();
    AspectList getAspects();
    AspectList getAspects(IInventory var1);
    String getResearch();

    
}
