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
	
	/**
	 * Returns the address of the Stargate nearest to the specified location, or null if there is no gate within the specified radius
	 * @param world The world the target Stargate is in.
	 * @param x The target Stargate's X coordinate.
	 * @param y The target Stargate's Y coordinate.
	 * @param z The target Stargate's Z coordinate.
	 * @param radius The maximum radius to look for a Stargate. Use -1 to search the whole world.
	 * @return The Stargate's address, or null if no Stargate was found
	 */
	public Address findNearestStargate(World world, int x, int y, int z, int radius);
	
	/**
	 * Register a new IDynamicWorldLoader.
	 * 
	 * @param dwl The IDynamicWorldLoader to register.
	 */
	public void registerDynamicWorldLoader(IDynamicWorldLoader dwl);
	
	/**
	 * Unregister a known IDynamicWorldLoader.
	 * 
	 * @param dwl The IDynamicWorldLoader to unregister.
	 */
	public void unregisterDynamicWorldLoader(IDynamicWorldLoader dwl);
	
	/**
	 * Reserve an address prefix for your DWL.
	 * If a Stargate attempts to dial a world with that prefix,
	 * your DWL is given exclusivity in generating that world.
	 * 
	 * @param dwl Your IDynamicWorldLoader
	 * @param prefix And array of exactly 3 non-null and non-void symbols representing a dimension.
	 * @return whether or not the prefix has been successfully reserved.
	 */
	public boolean reserveDimensionPrefix(IDynamicWorldLoader dwl, Symbol[] prefix);
	
	/**
	 * Checks if the specified prefix is associated with a dimension or reserved by a {@link IDynamicWorldLoader}.
	 * 
	 * @param prefix An array of exactly 3 non-null and non-void symbols representing a dimension.
	 * @return Whether or not the prefix is either used or reserved.
	 */
	public boolean prefixExists(Symbol[] prefix);
}