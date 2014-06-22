package stargatetech2.api.bus;

import java.util.ArrayList;

public final class BusProtocols {	
	private static final ArrayList<Class<? extends BusPacket>> protocols = new ArrayList();
	
	/**
	 * Add a protocol to the list, if it doesn't exist yet.
	 * 
	 * @param packetClass the class of the packet corresponding to the protocol we're adding.
	 * @return the id of the protocol we just added.
	 */
	public static final int addProtocol(Class<? extends BusPacket> packetClass){
		if(!protocols.contains(packetClass)){
			protocols.add(packetClass);
		}
		return protocols.indexOf(packetClass);
	}
	
	/**
	 * Gives you the id of the protocol correspondig to a given packet class.
	 * 
	 * @param packetClass the class of the packet for which we want to know the protocol ID.
	 * @return the ID of the protocol corresponding to the packet class.
	 */
	public static final int getProtocolID(Class<? extends BusPacket> packetClass){
		return protocols.indexOf(packetClass);
	}
	
	private BusProtocols(){}
	
	
	
	// A list of all the protocols implemented by StargateTech 2
	public static final int PROTOCOL_LIP = addProtocol(BusPacketLIP.class);
	public static final int PROTOCOL_NETSCAN = addProtocol(BusPacketNetScan.class);
}
