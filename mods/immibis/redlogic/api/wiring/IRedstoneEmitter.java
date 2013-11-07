package mods.immibis.redlogic.api.wiring;

/**
 * Implemented by tile entities that emit a full-strength red alloy signal.
 */
public interface IRedstoneEmitter {
	/**
	 * toDirection and blockFace are the same as the ones passed to IConnectable.
	 * blockFace is -1 for freestanding wire connections.
	 * 
	 * @return Signal strength from 0 to 255.
	 */ 
	public short getEmittedSignalStrength(int blockFace, int toDirection);
}
