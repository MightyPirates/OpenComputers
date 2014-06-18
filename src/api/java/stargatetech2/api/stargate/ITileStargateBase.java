package stargatetech2.api.stargate;

/**
 * Represents a Stargate base block (the block that supports the stargate).
 * 
 * It contains all the important logic in the Stargate,
 * like dialing, Iris control and power usage.
 * 
 * Because the ring is inside the block that supports it, it is possible to
 * call the same methods you can call on a ring.
 * 
 * @see ITileStargate
 * 
 * @author LordFokas
 */
public interface ITileStargateBase extends ITileStargate{
	public enum DialMethod{
		MANUAL,	// Dialing Computers
		AUTO	// DHDs
	}
	
	/**
	 * Used to try making the Stargate dial an address.
	 * 
	 * @param address The address this Stargate should dial.
	 * @param timeout How many seconds the connection will last. (1 - 38; default: 38);
	 * @return whether the dialing sequence started (true) or failed (false).
	 */
	public DialError dial(Address address, int timeout, DialMethod method);
}