package ic2.api.recipe;

import java.util.Arrays;
import java.util.List;

import net.minecraft.item.ItemStack;

import net.minecraftforge.oredict.OreDictionary;

public class RecipeInputItemStack implements IRecipeInput {
	public RecipeInputItemStack(ItemStack input) {
		this(input, input.stackSize);
	}

	public RecipeInputItemStack(ItemStack input, int amount) {
		this.input = input;
		this.amount = amount;
	}

	@Override
	public boolean matches(ItemStack subject) {
		return subject.itemID == input.itemID &&
				(subject.getItemDamage() == input.getItemDamage() || input.getItemDamage() == OreDictionary.WILDCARD_VALUE);
	}

	@Override
	public int getAmount() {
		return amount;
	}

	@Override
	public List<ItemStack> getInputs() {
		return Arrays.asList(input);
	}

	public final ItemStack input;
	public final int amount;
}
