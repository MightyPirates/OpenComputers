package ic2.api.item;

import net.minecraft.world.World;

/**
 * Allows an item to act as a terraformer blueprint.
 */
public interface ITerraformingBP
{
	/**
	 * Get the energy consumption per operation of the blueprint.
	 * 
	 * @return Energy consumption in EU
	 */
	public abstract int getConsume();
	
	/**
	 * Get the maximum range of the blueprint.
	 * Should be a divisor of 5.
	 * 
	 * @return Maximum range in blocks
	 */
	public abstract int getRange();
	
	/**
	 * Perform the terraforming operation.
	 * 
	 * @param world world to terraform
	 * @param x X position to terraform
	 * @param z Z position to terraform
	 * @param yCoord Y position of the terraformer
	 * @return Whether the operation was successful and the terraformer should consume energy.
	 */
	public abstract boolean terraform(World world, int x, int z, int yCoord);
}
