package stargatetech2.api.stargate;

import net.minecraft.world.World;

public interface IStargateNetwork {
	/**
	 * @return Whether the Stargate Network is loaded (working) or not.
	 */
	public boolean isLoaded();
	
	/**
	 * @param address The string representation of an address. (e.g. "Proclarush Taonas At")
	 * @return an address object if the string is a valid address, null otherwise.
	 */
	public Address parseAddress(String address);
	
	/**
	 * Checks if a given address exists in the network or not.
	 * (i.e., if this address maps to a physical Stargate)
	 * 
	 * @param address the address we want to check.
	 * @return whether the address exists or not.
	 */
	public boolean addressExists(Address address);
	
	/**
	 * Returns the address of the Stargate in a specific location if it exists or null otherwise.
	 * 
	 * @param world The world the target Stargate is in.
	 * @param x The target Stargate's X coordinate.
	 * @param y The target Stargate's Y coordinate.
	 * @param z The target Stargate's Z coordinate.
	 * @return The Stargate's address, or null if the location doesn't contain a Stargate.
	 */
	public Address getAddressOf(World world, int x, int y, int z);
}