package ic2.api.recipe;

import java.util.Arrays;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public final class RecipeOutput {
	public RecipeOutput(NBTTagCompound metadata, List<ItemStack> items) {
		this.metadata = metadata;
		this.items = items;
	}

	public RecipeOutput(NBTTagCompound metadata, ItemStack... items) {
		this(metadata, Arrays.asList(items));
	}

	public final List<ItemStack> items;
	public final NBTTagCompound metadata;
}
