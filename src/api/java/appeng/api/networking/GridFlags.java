package appeng.api.networking;

/**
 * Various flags to determine network node behavior.
 */
public enum GridFlags
{
	/**
	 * import/export buses, terminals, and other devices that use network features, will use this setting.
	 */
	REQUIRE_CHANNEL,

	/**
	 * MAC, and P2P ME tunnels use this setting.
	 */
	DENSE_CHANNEL,

	/**
	 * cannot carry channels over this node.
	 */
	CANNOT_CARRY,

	/**
	 * Used by P2P Tunnels to prevent tunnels from tunneling recursively.
	 */
	CANNOT_CARRY_DENSE,

	/**
	 * This block can transmit 32 signals, this should only apply to Tier2 Cable, P2P Tunnels, and Quantum Network Bridges.
	 */
	TIER_2_CAPACITY,

	/**
	 * This block is part of a multiblock, used in conjunction with REQUIRE_CANNEL, and {@link IGridMultiblock} see this
	 * interface for details.
	 */
	MULTIBLOCK
}
