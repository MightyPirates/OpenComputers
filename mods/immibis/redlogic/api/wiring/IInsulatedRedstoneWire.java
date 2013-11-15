package mods.immibis.redlogic.api.wiring;

/**
 * Marker interface for insulated wire tile entities.
 */
public interface IInsulatedRedstoneWire extends IRedstoneWire {
	/**
	 * Returns the colour as a standard Minecraft wool colour.
	 * 0=white, 15=black
	 */
	public int getInsulatedWireColour();
}
