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
