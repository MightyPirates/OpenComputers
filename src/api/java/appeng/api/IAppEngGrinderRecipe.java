package appeng.api;

import net.minecraft.item.ItemStack;

/**
 * Lets you manipulate existing recipes.
 */
public interface IAppEngGrinderRecipe {
	
	/**
	 * the current input
	 * @return input that the grinder will accept.
	 */
	public ItemStack getInput();
	
	/**
	 * lets you change the grinder recipe by changing its input.
	 * @param input
	 */
	public void setInput( ItemStack input );
	
	/**
	 * gets the current output
	 * @return output that the grinder will produce
	 */
	public ItemStack getOutput();
	
	/**
	 * allows you to change the output.
	 * @param output
	 */
	public void setOutput( ItemStack output );
	
	/**
	 * Energy cost, in turns.
	 * @return number of turns it takes to produce the output from the input.
	 */
	public int getEnergyCost();
	
	/**
	 * Allows you to adjust the number of turns 
	 * @param new number of turns to produce output.
	 */
	public void setEnergyCost(int c);
}
