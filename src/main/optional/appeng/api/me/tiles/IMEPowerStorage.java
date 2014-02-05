package appeng.api.me.tiles;

import appeng.api.config.ItemFlow;

/**
 * Used to access information about AE's various power accepting blocks for monitoring purposes.
 *
 */
public interface IMEPowerStorage {
	
	/**
	 * ME power storage asset, pass the amount you want to use, and what you want to use it for.
	 * @param use
	 * @param for_what
	 * @return if you can use it.
	 */
	boolean useMEEnergy( float use, String for_what );
	
	/**
	 * Add energy to an ME Power storage.
	 * @param amt that was not added to storage myEnergy = addMEPower( myEnergy ); // pushes your power into the network.
	 * @return
	 */
	public double addMEPower( double amt );
	
	/**
	 * returns the current maximum power ( this can change :P )
	 */
	public double getMEMaxPower();
	
	/**
	 * returns the current AE Power Level, this may exceed getMEMaxPower()
	 */
	public double getMECurrentPower();
	
	/**
	 * Checked on network reset to see if your block can be used as a public power storage ( use getPowerFlow to control the behavior )
	 * @return
	 */
	public boolean isPublicPowerStorage();
	
	/**
	 * Control the power flow by telling what the controller can do, either add? or subtract? or both!
	 * @return
	 */
	public ItemFlow getPowerFlow();
	
	/**
	 * Drains power from this storage, it returns the power it was able to obtain.
	 * @param amt - amount of power to drain.
	 * @return amount of power obtained.
	 */
	double drainMEPower(double amt);
	
}
