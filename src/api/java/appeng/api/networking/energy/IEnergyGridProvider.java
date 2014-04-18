package appeng.api.networking.energy;

import java.util.Set;

import appeng.api.config.Actionable;

/**
 * internal use only.
 */
public interface IEnergyGridProvider
{

	/**
	 * internal use only
	 */
	public double extractAEPower(double amt, Actionable mode, Set<IEnergyGrid> seen);

}
