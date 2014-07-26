/*******************************************************************************
 * Copyright 2011-2014 SirSengir
 * 
 * This work (the API) is licensed under the "MIT" License, see LICENSE.txt for details.
 ******************************************************************************/
package forestry.api.fuels;

import net.minecraft.item.ItemStack;

public class EngineCopperFuel {

	/**
	 * Item that is valid fuel for a peat-fired engine.
	 */
	public final ItemStack fuel;
	/**
	 * Power produced by this fuel per work cycle.
	 */
	public final int powerPerCycle;
	/**
	 * Amount of work cycles this item lasts before being consumed.
	 */
	public final int burnDuration;

	public EngineCopperFuel(ItemStack fuel, int powerPerCycle, int burnDuration) {
		this.fuel = fuel;
		this.powerPerCycle = powerPerCycle;
		this.burnDuration = burnDuration;
	}

}
