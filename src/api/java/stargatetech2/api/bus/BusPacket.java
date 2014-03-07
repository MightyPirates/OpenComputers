package stargatetech2.api.bus;

public abstract class BusPacket {
	private final short sender;
	private final short target;
	private final boolean hasLIP;
	
	/**
	 * @param sender The address of the Device that is sending this packet.
	 * @param target The address of the Device(s) that should receive this packet.
	 * @param hasLIP Whether or not this packet supports being converted to a plain text (LIP) format.
	 */
	protected BusPacket(short sender, short target, boolean hasLIP){
		this.sender = sender;
		this.target = target;
		this.hasLIP = hasLIP;
	}
	
	/**
	 * @return The address of the device that sent this packet.
	 */
	public final short getSender(){
		return sender;
	}
	
	/**
	 * @return The address of the device(s) that should receive this packet.
	 */
	public final short getTarget(){
		return target;
	}
	
	/**
	 * @return The ID of the protocol this packet corresponds to.
	 */
	public final int getProtocolID(){
		return BusProtocols.getProtocolID(this.getClass());
	}
	
	/**
	 * @return A plain text (LIP) version of this packet, if it has one.
	 */
	public final BusPacketLIP getPlainText(){
		if(this instanceof BusPacketLIP){
			return (BusPacketLIP) this;
		}else if(hasLIP){
			BusPacketLIP lip = new BusPacketLIP(sender, target);
			fillPlainText(lip);
			lip.finish();
			return lip;
		}
		return null;
	}
	
	/**
	 * Used by subclasses to convert themselves to plain text format.
	 * 
	 * @param lip The Lazy Intercom Protocol (LIP) packet we're filling with our data.
	 */
	protected abstract void fillPlainText(BusPacketLIP lip);
	
	/**
	 * @return Whether or not this packet supports conversion to the LIP format.
	 */
	public final boolean hasPlainText(){
		return hasLIP;
	}
}
