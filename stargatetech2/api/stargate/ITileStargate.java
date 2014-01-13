package stargatetech2.api.stargate;

/**
 * Represents a Stargate ring.
 * Stargate "base" blocks contain the ring, so they implement this as well.
 * 
 * All you can get from the Stargate ring is the address.
 * 
 * @author LordFokas
 */
public interface ITileStargate {
	/**
	 * @return This Stargate's address. null values are possible on the client side.
	 */
	public Address getAddress();
}