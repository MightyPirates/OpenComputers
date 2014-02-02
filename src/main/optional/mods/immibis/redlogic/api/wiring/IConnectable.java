package mods.immibis.redlogic.api.wiring;

/**
 * Interface implemented by tile entities which can connect to wires in specific ways.
 * 
 * Note: Although wires implement IConnectable, do not use IConnectable to determine if a wire
 * is connected to you, as IConnectable only exposes possible connections, and there are reasons
 * for wires to not connect (e.g. if there is a microblock in the way).
 * Use {@link IWire#wireConnectsInDirection(int, int)} to determine the actual connections of a wire.
 */
public interface IConnectable {
	/**
	 * Called to check whether a wire connects to this block.
	 * @param wire The wire being checked.
	 * @param blockFace The face the wire is on in the block containing it.
	 * 					-1 when checking for jacketed wire connections.
	 * @param fromDirection The direction the wire block is in, relative to this block.
	 * @return True to allow the wire connection.
	 */
	public boolean connects(IWire wire, int blockFace, int fromDirection);

	/**
	 * Called to check whether a wire connects to this block from around a corner (actually an edge)
	 * 
	 * Example:
	 * 
	 * <pre>
	 *   A#
	 *    B
	 * </pre>
	 * In this situation, where A is the wire, # is a solid block and B is this block, X is right and Y is down,
	 * blockFace is -Y (up) and fromDirection is -X (left).
	 * 
	 * Note this is different from:
	 * <pre>
	 *   A
	 *   #B
	 * </pre>
	 * where blockFace is -X and fromDirection is -Y.
	 * 
	 * @param wire The wire being checked.
	 * @param blockFace The face of *this block* the edge is adjacent to.
	 * @param fromDirection The side the edge is on within that face.
	 * @return True to allow the wire connection.
	 */
	public boolean connectsAroundCorner(IWire wire, int blockFace, int fromDirection);
}
