package atomicscience.api;

import net.minecraft.world.World;

public interface IAntiPoisonBlock
{
	/**
	 * Returns true if this armor prevents poison from passing through.
	 * 
	 * @return
	 */
	public boolean isPoisonPrevention(World par1World, int x, int y, int z, String type);
}
