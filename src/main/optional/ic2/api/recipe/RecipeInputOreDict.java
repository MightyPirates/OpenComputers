package ic2.api.recipe;

import java.util.List;

import net.minecraft.item.ItemStack;

import net.minecraftforge.oredict.OreDictionary;

public class RecipeInputOreDict implements IRecipeInput {
	public RecipeInputOreDict(String input1) {
		this(input1, 1);
	}

	public RecipeInputOreDict(String input1, int amount1) {
		this.input = input1;
		this.amount = amount1;
	}

	@Override
	public boolean matches(ItemStack subject) {
		List<ItemStack> inputs = OreDictionary.getOres(input);

		for (ItemStack input1 : inputs) {
			if (subject.itemID == input1.itemID &&
					(subject.getItemDamage() == input1.getItemDamage() || input1.getItemDamage() == OreDictionary.WILDCARD_VALUE)) {
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
