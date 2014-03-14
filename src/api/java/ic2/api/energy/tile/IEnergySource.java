package ic2.api.energy.tile;

/**
 * Allows a tile entity (mostly a generator) to emit energy.
 * 
 * See ic2/api/energy/usage.txt for an overall description of the energy net api.
 */
public interface IEnergySource extends IEnergyEmitter {
	/**
	 * Energy output provided by the source this tick.
	 * This is typically Math.min(stored energy, max output/tick).
	 * 
	 * @return Energy offered this tick
	 */
	double getOfferedEnergy();

	/**
	 * Draw energy from this source's buffer.
	 * 
	 * If the source doesn't have a buffer, this is a no-op.
	 * 
	 * @param amount amount of EU to draw, may be negative
	 */
	void drawEnergy(double amount);
}

