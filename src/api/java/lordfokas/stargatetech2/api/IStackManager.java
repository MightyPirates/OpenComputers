package stargatetech2.api;

import java.util.Collection;

import net.minecraft.item.ItemStack;

/**
 * The way to get ItemStacks added to the game by StargateTech 2
 * 
 * @author LordFokas
 */
public interface IStackManager {
	/**
	 * Used to fetch an ItemStack by it's name, with a default size of 1.
	 * 
	 * @param stack The stack we want to fetch.
	 * @return The stack, or null if none was found.
	 */
	public ItemStack get(String stack);
	
	/**
	 * Used to fetch an ItemStack by it's name with a given size.
	 * 
	 * @param stack The stack we want to fetch.
	 * @param size The size the stack comes with. Must be in the range 1 - 64.
	 * @return The stack, or null if none was found.
	 */
	public ItemStack get(String stack, int size);
	
	/**
	 * @return A list with the names of all the existing stacks.
	 */
	public Collection<String> getAllStacks();
}