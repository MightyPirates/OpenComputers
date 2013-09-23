package ic2.api.energy.event;

import ic2.api.energy.tile.IEnergyTile;

/**
 * Event announcing terminated energy tiles.
 *
 * This event notifies subscribers of unloaded energy tiles, e.g. after getting
 * unloaded through the chunk they are in or after being destroyed by the
 * player or another block pick/destruction mechanism.
 *
 * Every energy tile which wants to get disconnected from the IC2 Energy
 * Network has to either post this event or alternatively call
 * EnergyNet.removeTileEntity().
 *
 * You may use this event to build a static representation of energy tiles for
 * your own energy grid implementation if you need to. It's not required if you
 * always lookup energy paths on demand.
 * 
 * See ic2/api/energy/usage.txt for an overall description of the energy net api.
 */
public class EnergyTileUnloadEvent extends EnergyTileEvent {
	public EnergyTileUnloadEvent(IEnergyTile energyTile) {
		super(energyTile);
	}
}

