package ic2.api.recipe;

import java.util.List;

import net.minecraft.item.ItemStack;

/**
 * Recipe manager interface for basic lists.
 * 
 * @author Richard
 */
public interface IListRecipeManager extends Iterable<ItemStack> {
	/**
	 * Adds a stack to the list.
	 * 
	 * @param stack Stack to add
	 */
	public void add(ItemStack stack);
	
	/**
	 * Checks whether the specified stack is in the list.
	 * 
	 * @param stack Stack to check
	 * @return Whether the stack is in the list
	 */
	public boolean contains(ItemStack stack);
	
	/**
	 * Gets the list of stacks.
	 * 
	 * You're a mad evil scientist if you ever modify this.
	 * 
	 * @return List of stacks
	 */
	public List<ItemStack> getStacks();
}
