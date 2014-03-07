package mods.immibis.redlogic.api.wiring;

/**
 * Implemented by entities that can emit bundled cable signals.
 */
public interface IBundledEmitter {
	/**
	 * Returns the current emitted bundled cable strength for each colour.
	 * The bytes are treated as having unsigned values from 0 to 255 - when extracting
	 * a value from the array, you need to convert it (value & 255).
	 * 
	 * May return null, which is equivalent to returning an array with all values 0.
	 * 
	 * Array indices are the same as the corresponding wool damage values.
	 * 
	 * blockFace and toDirection are the same as the values passed to IRedstoneEmitter and IConnectable.
	 * blockFace can be -1 for freestanding wire connections.
	 * 
	 * The return value will be used immediately, so the returned array may be overwritten
	 * by the next call to getBundledCableStrength.
	 */
	public byte[] getBundledCableStrength(int blockFace, int toDirection);
}
