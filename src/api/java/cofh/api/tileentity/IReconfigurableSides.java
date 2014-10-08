package cofh.api.tileentity;

/**
 * Implement this interface on Tile Entities which allow for reconfiguration of their sides.
 * 
 * Coordination with the containing block is required.
 * 
 * @author King Lemming
 * 
 */
public interface IReconfigurableSides {

	/**
	 * Decrement the config for a given side.
	 * 
	 * @param side
	 *            The side to decrement.
	 * @return True if config was changed, false otherwise.
	 */
	boolean decrSide(int side);

	/**
	 * Increment the config for a given side.
	 * 
	 * @param side
	 *            The side to decrement.
	 * @return True if config was changed, false otherwise.
	 */
	boolean incrSide(int side);

	/**
	 * Set the config for a given side.
	 * 
	 * @param side
	 *            The side to set.
	 * @param config
	 *            The config value to use.
	 * @return True of config was set, false otherwise.
	 */
	boolean setSide(int side, int config);

	/**
	 * Reset configs on all sides to their base values.
	 * 
	 * @return True if reset was successful, false otherwise.
	 */
	boolean resetSides();

	/**
	 * Returns the number of possible config settings for a given side.
	 */
	int getNumConfig(int side);

}
