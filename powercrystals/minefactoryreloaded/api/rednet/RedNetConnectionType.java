package powercrystals.minefactoryreloaded.api.rednet;

import java.util.HashMap;
import java.util.Map;

/**
 * Defines how RedNet cable connects to a block
 * 
 * None:				RedNet will never connect to this block (if this is all you want: use IRedNetNoConnection)
 * 
 * CableSingle:			Connections will use the cable renderer with a single band, best used for whole blocks
 * PlateSingle:			Connections will use the plate renderer with a single band, used for conveyers and rails
 * 
 * CableAll:			Connections permit access to all 16 bands
 * PlateAll:			Connections permit access to all 16 bands
 * 
 * Forced connection modes are best used for decoration blocks: RedNet will not connect normally, but will if the user forces it
 *    Typically, IRedNetDecorative is desired for this instead
 * 
 * ForcedCableSingle:	Connections permit access to a single band only when the cable is in forced connection mode
 * ForcedPlateSingle:	Connections permit access to a single band only when the cable is in forced connection mode
 * 
 * ForcedCableAll:		Connections permit access to all 16 bands only when the cable is in forced connection mode
 * ForcedPlateAll:		Connections permit access to all 16 bands only when the cable is in forced connection mode
 */
public enum RedNetConnectionType
{
	None,				// 0
	CableSingle,		// 11
	PlateSingle,		// 13
	CableAll,			// 19
	PlateAll,			// 21
	ForcedCableSingle,	// 43
	ForcedPlateSingle,	// 45
	ForcedCableAll,		// 51
	ForcedPlateAll;		// 53
	
	public final boolean isConnected = this.ordinal() != 0;
	public final boolean isSingleSubnet = this.name().endsWith("Single");
	public final boolean isAllSubnets = this.name().endsWith("All");
	public final boolean isConnectionForced = this.name().startsWith("Forced");
	public final boolean isPlate = this.name().contains("Plate");
	public final boolean isCable = this.name().contains("Cable");
	public final short flags = toFlags(isConnected, isCable, isPlate, isSingleSubnet, isAllSubnets, isConnectionForced);
	
	public static final RedNetConnectionType fromFlags(short flags)
	{
		return connections.get(flags);
	}
	
	private static final short toFlags(boolean ...flags)
	{
		short ret = 0;
		for (int i = flags.length; i --> 0;)
			ret |= (flags[i] ? 1 : 0) << i;
		return ret;
	}
	
	private static final Map<Short, RedNetConnectionType> connections = new HashMap<Short, RedNetConnectionType>();
	
	static {
		for (RedNetConnectionType type : RedNetConnectionType.values())
			connections.put(type.flags, type);
	}
}
