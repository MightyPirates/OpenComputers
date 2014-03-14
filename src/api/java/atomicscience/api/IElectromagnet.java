package atomicscience.api;

import net.minecraft.world.World;

/**
 * Applied to all blocks that are to act like an electromagnet
 */
public interface IElectromagnet
{

	/**
	 * Is this electromagnet working currently?
	 */
	public boolean isRunning(World world, int x, int y, int z);
}
