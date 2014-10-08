package cofh.api.tileentity;

/**
 * Implement this interface on objects which can report information about their energy usage.
 * 
 * This is used for reporting purposes - Energy transactions are handled through IEnergyHandler!
 * 
 * @author King Lemming
 * 
 */
public interface IEnergyInfo {

	/**
	 * Returns energy usage/generation per tick (RF/t).
	 */
	int getInfoEnergyPerTick();

	/**
	 * Returns maximum energy usage/generation per tick (RF/t).
	 */
	int getInfoMaxEnergyPerTick();

	/**
	 * Returns energy stored (RF).
	 */
	int getInfoEnergyStored();

	/**
	 * Returns maximum energy stored (RF).
	 */
	int getInfoMaxEnergyStored();

}
