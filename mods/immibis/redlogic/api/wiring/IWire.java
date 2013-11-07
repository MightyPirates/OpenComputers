package mods.immibis.redlogic.api.wiring;

/**
 * Marker interface for wire tile entities.
 * Other mods should not implement this, nor any sub-interface.
 */
public interface IWire {
	/**
	 * blockFace can be -1 to check freestanding wire connections.
	 * @see mods.immibis.redlogic.api.wiring.IConnectable
	 */
	public boolean wireConnectsInDirection(int blockFace, int direction);
	
	/**
	 * Same as wireConnectsInDirection, but only checks for connections around an external corner.
	 */
	public boolean wireConnectsInDirectionAroundCorner(int blockFace, int direction);
}
