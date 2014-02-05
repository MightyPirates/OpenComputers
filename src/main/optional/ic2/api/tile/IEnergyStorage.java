package ic2.api.tile;

import net.minecraftforge.common.ForgeDirection;

/**
 * Interface implemented by the tile entity of energy storage blocks.
 */
public interface IEnergyStorage {
	/**
	 * Get the amount of energy currently stored in the block.
	 * 
	 * @return Energy stored in the block
	 */
	public int getStored();

	/**
	 * Set the amount of energy currently stored in the block.
	 * 
	 * @param energy stored energy
	 */
	public void setStored(int energy);

	/**
	 * Add the specified amount of energy.
	 * 
	 * Use negative values to decrease.
	 * 
	 * @param amount of energy to add
	 * @return Energy stored in the block after adding the specified amount
	 */
	public int addEnergy(int amount);

	/**
	 * Get the maximum amount of energy the block can store.
	 * 
	 * @return Maximum energy stored
	 */
	public int getCapacity();

	/**
	 * Get the block's energy output.
	 * 
	 * @return Energy output in EU/t
	 */
	public int getOutput();

	/**
	 * Get the block's energy output.
	 * 
	 * @return Energy output in EU/t
	 */
	public double getOutputEnergyUnitsPerTick();

	/**
	 * Get whether this block can have its energy used by an adjacent teleporter.
	 * 
	 * @param side side the teleporter is draining energy from
	 * @return Whether the block is teleporter compatible
	 */
	public boolean isTeleporterCompatible(ForgeDirection side);
}
