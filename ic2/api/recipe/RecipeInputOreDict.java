package ic2.api.recipe;

import java.util.List;

import net.minecraft.item.ItemStack;

import net.minecraftforge.oredict.OreDictionary;

public class RecipeInputOreDict implements IRecipeInput {
	public RecipeInputOreDict(String input) {
		this(input, 1);
	}

	public RecipeInputOreDict(String input, int amount) {
		this.input = input;
		this.amount = amount;
	}

	@Override
	public boolean matches(ItemStack subject) {
		List<ItemStack> inputs = OreDictionary.getOres(input);

		for (ItemStack input : inputs) {
			if (subject.itemID == input.itemID &&
					(subject.getItemDamage() == input.getItemDamage() || input.getItemDamage() == OreDictionary.WILDCARD_VALUE)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public int getAmount() {
		return amount;
	}

	@Override
	public List<ItemStack> getInputs() {
		return OreDictionary.getOres(input);
	}

	public final String input;
	public final int amount;
}
