package stargatetech2.api.bus;

/**
 * This provides a level of abstraction over the IBusInterface.
 * Implement to your own liking.
 * 
 * @author LordFokas
 */
public interface IBusDriver {
	/**
	 * Used to check if this device should receive a specific
	 * packet type from this sender. If true is returned,
	 * handlePacket() is called afterwards.
	 * 
	 * @param sender The Bus address of the packet's sender.
	 * @param protocolID The unique ID of this packet type.
	 * @param hasLIP whether the packet can be converted to the LIP format or not.
	 * @return Whether the device will accept this packet or not.
	 */
	public boolean canHandlePacket(short sender, int protocolID, boolean hasLIP);
	
	/**
	 * Called by the network to have this device handle a packet.
	 * 
	 * @param packet The packet to be handled.
	 */
	public void handlePacket(BusPacket packet);
	
	/**
	 * Used to make the IBusDriver give all packets in its send
	 * queue, one by one, to the IBusInterface so that it can
	 * send them accross the network.
	 * 
	 * @return The next BusPacket in the queue, if any, null otherise.
	 */
	public BusPacket getNextPacketToSend();
	
	/**
	 * Called by the hardware representation (IBusInterface)
	 * to check if it's active or not.
	 * Inactive interfaces cannot send or receive packets.
	 * 
	 * @return Whether this interface is active or not.
	 */
	public boolean isInterfaceEnabled();
	
	/**
	 * Called by this Driver's Interface to check it's own address.
	 * 
	 * @return The address of this IBusDriver's IBusInterface.
	 */
	public short getInterfaceAddress();
	
	/**
	 * @return this driver's short name.<br>
	 * Should be readable and indicate what kind of device it is.<br>
	 * <b>Example:</b> Shield Controller
	 */
	public String getShortName();
	
	/**
	 * @return a short description of what this device is.
	 */
	public String getDescription();
}