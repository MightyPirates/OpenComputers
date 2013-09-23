package ic2.api.energy;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import net.minecraftforge.common.ForgeDirection;

/**
 * Interface representing the methods provided by the global EnergyNet class.
 * 
 * See ic2/api/energy/usage.txt for an overall description of the energy net api.
 */
public interface IEnergyNet {
	/**
	 * Get the EnergyNet-registered tile entity at the specified position.
	 * 
	 * This is not the same as World.getBlockTileEntity(), it's possible to register delegate tile
	 * entities with the energy net which are different from what's actually in the world. Those
	 * delegates allow to use separate TileEntity objects just for the EnergyNet interfaces,
	 * simplifying cross-mod dependencies and multi-blocks.
	 * 
	 * @param world World containing the tile entity
	 * @param x x-coordinate
	 * @param y y-coordinate
	 * @param z z-coordinate
	 * @return tile entity registered to the energy net or null if none is registered
	 */
	TileEntity getTileEntity(World world, int x, int y, int z);

	/**
	 * Get the EnergyNet-registered neighbor tile entity at the specified position.
	 * 
	 * @param te TileEntity indicating the world and position to search from
	 * @param dir direction the neighbor is to be found
	 * @return neighbor tile entity registered to the energy net or null if none is registered
	 */
	TileEntity getNeighbor(TileEntity te, ForgeDirection dir);

	/**
	 * determine how much energy has been emitted by the EnergyEmitter specified
	 *
	 * @note call this twice with x ticks delay to get the avg. emitted power p = (call2 - call1) / x EU/tick
	 *
	 * @param tileEntity energy emitter
	 */
	long getTotalEnergyEmitted(TileEntity tileEntity);

	/**
	 * determine how much energy has been sunken by the EnergySink specified
	 *
	 * @note call this twice with x ticks delay to get the avg. sunken power p = (call2 - call1) / x EU/tick
	 *
	 * @param tileEntity energy emitter
	 */
	long getTotalEnergySunken(TileEntity tileEntity);

	/**
	 * Determine the typical power used by the specific tier, e.g. 128 eu/t for tier 2.
	 * 
	 * @param tier tier
	 * @return power in eu/t
	 */
	int getPowerFromTier(int tier);
}
