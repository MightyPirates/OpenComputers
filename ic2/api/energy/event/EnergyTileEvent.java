package ic2.api.energy.event;

import net.minecraft.tileentity.TileEntity;

import net.minecraftforge.event.world.WorldEvent;

import ic2.api.energy.tile.IEnergyTile;

/**
 * Base class for energy net events, don't use it directly.
 * 
 * See ic2/api/energy/usage.txt for an overall description of the energy net api.
 */
public class EnergyTileEvent extends WorldEvent {
	public final IEnergyTile energyTile;

	public EnergyTileEvent(IEnergyTile energyTile) {
		super(((TileEntity) energyTile).worldObj);

		this.energyTile = energyTile;
	}
}

