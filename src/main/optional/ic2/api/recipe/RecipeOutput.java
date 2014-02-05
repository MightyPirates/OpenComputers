package ic2.api.recipe;

import java.util.Arrays;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public final class RecipeOutput {
	public RecipeOutput(NBTTagCompound metadata1, List<ItemStack> items1) {
		this.metadata = metadata1;
		this.items = items1;
	}

	public RecipeOutput(NBTTagCompound metadata1, ItemStack... items1) {
		this(metadata1, Arrays.asList(items1));
	}

	public final List<ItemStack> items;
	public final NBTTagCompound metadata;
}
