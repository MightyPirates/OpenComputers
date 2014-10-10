package cofh.api.tileentity;

/**
 * Implement this interface on Tile Entities which allow for reconfiguration of their facing.
 * 
 * Coordination with the containing block is required.
 * 
 * @author King Lemming
 * 
 */
public interface IReconfigurableFacing {

	/**
	 * Returns the current facing of the block.
	 */
	int getFacing();

	/**
	 * Returns whether or not the block's face can be aligned with the Y Axis.
	 */
	boolean allowYAxisFacing();

	/**
	 * Attempt to rotate the block. Arbitrary based on implementation.
	 * 
	 * @return True if rotation was successful, false otherwise.
	 */
	boolean rotateBlock();

	/**
	 * Set the facing of the block.
	 * 
	 * @param side
	 *            The side to set the facing to.
	 * @return True if the facing was set, false otherwise.
	 */
	boolean setFacing(int side);

}
