/*******************************************************************************
 * Copyright 2011-2014 SirSengir
 * 
 * This work (the API) is licensed under the "MIT" License, see LICENSE.txt for details.
 ******************************************************************************/
package forestry.api.fuels;

import net.minecraftforge.fluids.Fluid;

public class EngineBronzeFuel {
	/**
	 * Item that is valid fuel for a biogas engine.
	 */
	public final Fluid liquid;
	/**
	 * Power produced by this fuel per work cycle of the engine.
	 */
	public final int powerPerCycle;
	/**
	 * How many work cycles a single "stack" of this type lasts.
	 */
	public final int burnDuration;
	/**
	 * By how much the normal heat dissipation rate of 1 is multiplied when using this fuel type.
	 */
	public final int dissipationMultiplier;

	public EngineBronzeFuel(Fluid liquid, int powerPerCycle, int burnDuration, int dissipationMultiplier) {
		this.liquid = liquid;
		this.powerPerCycle = powerPerCycle;
		this.burnDuration = burnDuration;
		this.dissipationMultiplier = dissipationMultiplier;
	}
}
