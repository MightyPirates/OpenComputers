package ic2.api.energy.event;

import ic2.api.energy.tile.IEnergyTile;

/**
 * Event announcing new energy tiles.
 *
 * This event notifies subscribers of loaded energy tiles, e.g. after getting
 * loaded through the chunk they are in or after being placed down by the
 * player or another deployer mechanism.
 *
 * Every energy tile which wants to get connected to the IC2 Energy Network has
 * to either post this event or alternatively call EnergyNet.addTileEntity().
 *
 * You may use this event to build a static representation of energy tiles for
 * your own energy grid implementation if you need to. It's not required if you
 * always lookup energy paths on demand.
 * 
 * See ic2/api/energy/usage.txt for an overall description of the energy net api.
 */
public class EnergyTileLoadEvent extends EnergyTileEvent {
	public EnergyTileLoadEvent(IEnergyTile energyTile) {
		super(energyTile);
	}
}

