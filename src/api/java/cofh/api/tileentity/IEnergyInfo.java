package cofh.api.tileentity;

/**
 * Implement this interface on Tile Entities which can report information about their energy usage.
 * 
 * This is used for reporting purposes - Energy transactions should be handled through IEnergyHandler!
 * 
 * @author King Lemming
 * 
 */
public interface IEnergyInfo {

	public int getEnergyPerTick();

	public int getMaxEnergyPerTick();

	public int getEnergy();

	public int getMaxEnergy();

}
