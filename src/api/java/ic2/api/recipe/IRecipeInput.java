package ic2.api.recipe;

import java.util.List;

import net.minecraft.item.ItemStack;

public interface IRecipeInput {
	/**
	 * Check if subject matches this recipe input, ignoring the amount.
	 * 
	 * @param subject ItemStack to check
	 * @return true if it matches the requirement
	 */
	boolean matches(ItemStack subject);

	/**
	 * Determine the minimum input stack size.
	 * 
	 * @return input amount required
	 */
	int getAmount();

	/**
	 * List all possible inputs (best effort).
	 * 
	 * The stack size is undefined, use getAmount to get the correct one.
	 * 
	 * @return list of inputs, may be incomplete
	 */
	List<ItemStack> getInputs();
}
