package stargatetech2.api.stargate;


public interface IDynamicWorldLoader {
	/**
	 * @param address The address we're creating a new world for.
	 * @return Whether or not this loader will create a new world for this non-existing address.
	 */
	public boolean willCreateWorldFor(Address address);
	
	/**
	 * Actually create a new world for this address.
	 * <b>This world must not exist already!</b>
	 * Do not forget to use the seedingShip to place a stargate on this world, or the pending
	 * wormhole will not connect.
	 * 
	 * @param address The address we're creating a new world for.
	 * @param seedingShip The IStargatePlacer we'll use to place our stargate.
	 */
	public void loadWorldFor(Address address, IStargatePlacer seedingShip);
}