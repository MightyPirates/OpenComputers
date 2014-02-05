package appeng.api.me.util;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;
import appeng.api.IAEItemStack;
import appeng.api.IItemList;

public interface IMEInventoryUtil {
	
	// ignores meta data - is always Fuzzy...
	// will only remove single items, this ignores your stack size...
	public ItemStack extractItemsByRecipe(World w, IRecipe r, ItemStack output, InventoryCrafting ci, ItemStack providedTemplate, int slot, IItemList all );
	
	// ignores meta data - is always Fuzzy...
	//public ItemStack extractItemsByType( ItemStack is ); // will only remove single items, this ignores your stack size...
	
}
