package buildcraft.api.recipes;

import java.util.ArrayList;
import java.util.LinkedList;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

public class AssemblyRecipe {

	public static LinkedList<AssemblyRecipe> assemblyRecipes = new LinkedList<AssemblyRecipe>();
	public final Object[] input;
	public final ItemStack output;
	public final float energy;

	public AssemblyRecipe(ItemStack[] input, int energy, ItemStack output) {
		this.input = input;
		this.output = output;
		this.energy = energy;
	}

	/**
	 * This version of AssemblyRecipe supports the OreDictionary
	 *
	 * @param input Object... containing either an ItemStack, or a paired string
	 * and integer(ex: "dyeBlue", 1)
	 * @param energy MJ cost to produce
	 * @param output resulting ItemStack
	 */
	public AssemblyRecipe(int energy, ItemStack output, Object... input) {
		this.output = output;
		this.energy = energy;
		this.input = input;

		for (int i = 0; i < input.length; i++) {
			if (input[i] instanceof String) {
				input[i] = OreDictionary.getOres((String) input[i]);
			}
		}
	}

	public boolean canBeDone(ItemStack... items) {
		for (int i = 0; i < input.length; i++) {
			if (input[i] == null)
				continue;

			if (input[i] instanceof ItemStack) {
				ItemStack requirement = (ItemStack) input[i];
				int found = 0; // Amount of ingredient found in inventory
				int expected = requirement.stackSize;
				for (ItemStack item : items) {
					if (item == null)
						continue;

					if (item.isItemEqual(requirement))
						found += item.stackSize; // Adds quantity of stack to amount found

				}

				// Return false if the amount of ingredient found
				// is not enough
				if (found < expected)
					return false;
			} else if (input[i] instanceof ArrayList) {
				ArrayList<ItemStack> oreList = (ArrayList<ItemStack>) input[i];
				int found = 0; // Amount of ingredient found in inventory
				int expected = (Integer) input[i++ + 1];

				for (ItemStack item : items) {
					if (item == null)
						continue;
					for (ItemStack oreItem : oreList) {
						if (OreDictionary.itemMatches(oreItem, item, true)) {
							found += item.stackSize;
							break;
						}
					}
				}

				// Return false if the amount of ingredient found
				// is not enough
				if (found < expected)
					return false;
			}
		}

		return true;
	}
}
