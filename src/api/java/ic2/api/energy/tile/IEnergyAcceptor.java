package ic2.api.energy.tile;

import net.minecraft.tileentity.TileEntity;

import net.minecraftforge.common.ForgeDirection;

/**
 * For internal/multi-block usage only.
 *
 * @see IEnergySink
 * @see IEnergyConductor
 * 
 * See ic2/api/energy/usage.txt for an overall description of the energy net api.
 */
public interface IEnergyAcceptor extends IEnergyTile {
	/**
	 * Determine if this acceptor can accept current from an adjacent emitter in a direction.
	 * 
	 * The TileEntity in the emitter parameter is what was originally added to the energy net,
	 * which may be normal in-world TileEntity, a delegate or an IMetaDelegate.
	 * 
	 * @param emitter energy emitter
	 * @param direction direction the energy is being received from
	 */
	boolean acceptsEnergyFrom(TileEntity emitter, ForgeDirection direction);
}

