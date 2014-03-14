package appeng.api;

/**
 * A Registry for locatable items, works based on serial numbers.
 */
public interface ILocateableRegistry {

	/**
	 * Attempts to find the object with the serial specifed, if it can it returns the object.
	 * @param ser
	 * @return requestedObject, or null
	 */
	public abstract Object findLocateableBySerial(long ser);

}