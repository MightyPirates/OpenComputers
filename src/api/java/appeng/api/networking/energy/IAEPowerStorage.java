package appeng.api.networking.energy;

import appeng.api.config.AccessRestriction;
import appeng.api.config.Actionable;

/**
 * Used to access information about AE's various power accepting blocks for monitoring purposes.
 */
public interface IAEPowerStorage extends IEnergySource
{

	/**
	 * Inject amt, power into the device, it will store what it can, and return the amount unable to be stored.
	 * 
	 * @param simulate
	 * 
	 * @return
	 */
	public double injectAEPower(double amt, Actionable mode);

	/**
	 * @return the current maximum power ( this can change :P )
	 */
	public double getAEMaxPower();

	/**
	 * @return the current AE Power Level, this may exceed getMEMaxPower()
	 */
	public double getAECurrentPower();

	/**
	 * Checked on network reset to see if your block can be used as a public power storage ( use getPowerFlow to control
	 * the behavior )
	 * 
	 * @return
	 */
	public boolean isAEPublicPowerStorage();

	/**
	 * Control the power flow by telling what the network can do, either add? or subtract? or both!
	 * 
	 * @return
	 */
	public AccessRestriction getPowerFlow();

}