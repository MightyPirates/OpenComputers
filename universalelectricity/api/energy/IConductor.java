package universalelectricity.api.energy;

import universalelectricity.api.net.IConnector;

/**
 * A connector for {EnergyNetwork}.
 * 
 * @author Calclavia
 */
public interface IConductor extends IConnector<IEnergyNetwork>, IEnergyInterface
{
	/**
	 * Gets the amount of resistance of energy conducting pass this conductor.
	 * 
	 * @return The amount of loss in Ohms.
	 */
	public float getResistance();

	/**
	 * The maximum amount of current this conductor can buffer (the transfer rate, essentially). You
	 * can simply do divide your energy transfer rate by UniversalElectricity.DEFAULT_VOLTAGE if
	 * your conductor is not voltage sensitive.
	 * 
	 * @return The amount of current in amperes.
	 */
	public long getCurrentCapacity();
}
