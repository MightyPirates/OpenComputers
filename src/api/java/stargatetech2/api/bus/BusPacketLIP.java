package stargatetech2.api.bus;

import java.util.ArrayList;
import java.util.Hashtable;

/**
 * LIP - Lazy Intercom Protocol<br>
 * <br>
 * Baptized by sciguyryan (often found in #ThermalExpansion and #StargateTech on esper.net's IRC),
 * this is the universal plain text format used in the Abstract Bus.<br>
 * <br>
 * Any packet can choose to be convertible to this format, allowing for any class anywhere to
 * read data from a packet which has an unknown and / or private class by asking it to convert to
 * a text format. This removes all the problems with type casting and such.
 * 
 * @author LordFokas
 */
public final class BusPacketLIP extends BusPacket<String> {
	private boolean isEditable = true;
	private LIPMetadata metadata = null;
	private Hashtable<String, String> data = new Hashtable();
	
	/**
	 * Defines optional metadata that helps sorting this packet out / figuring out what to do with this.
	 * The fact this is a plain text protocol makes the possibilities so vague it'd be impossible to
	 * guess what kind of data this packet carries.
	 * 
	 * @author LordFokas
	 */
	public static final class LIPMetadata{
		public final String modID;
		public final String deviceName;
		public final String playerName;
		
		/**
		 * Example: new LIPMetadata("StargateTech2", "shieldEmitter", "LordFokas");
		 * 
		 * @param modID The ID of the mod that generated this packet. PLEASE do fill this one.
		 * @param deviceName The name (or a unique identifier) of the type of machine that generated this.
		 * @param playerName The name of the player that made this packet be generated, if any.
		 */
		public LIPMetadata(String modID, String deviceName, String playerName){
			this.modID = modID;
			this.deviceName = deviceName;
			this.playerName = playerName;
		}
	}
	
	public BusPacketLIP(short sender, short target) {
		super(sender, target, true);
	}
	
	/**
	 * @param metadata The LIPMetadata object to set to this packet.
	 */
	public void setMetadata(LIPMetadata metadata){
		if(isEditable && this.metadata == null){
			this.metadata = metadata;
		}
	}
	
	/**
	 * @return The LIPMetadata object on this object. May be null.
	 */
	public LIPMetadata getMetadata(){
		return metadata;
	}

	@Override // We don't need this. At all.
	protected void fillPlainText(BusPacketLIP lip){}
	
	/**
	 * Finish creating this packet.
	 * As soon as you call this, it can no longer be modified.
	 */
	public void finish(){
		isEditable = false;
	}
	
	/**
	 * @return A list of all the keys for the data on this packet.
	 */
	public ArrayList<String> getEntryList(){
		ArrayList<String> entries = new ArrayList();
		entries.addAll(data.keySet());
		return entries;
	}
	
	/**
	 * Add a new entry to this packet.
	 * If the key already exists the entry is ignored.
	 * 
	 * @param key The key under which to send the data.
	 * @param val The data to send.
	 */
	public void set(String key, String val){
		if(isEditable && !data.containsKey(key)){
			data.put(key.toLowerCase(), val);
		}
	}
	
	/**
	 * Get the value stored under that key, if any.
	 * Case Insensitive.
	 * 
	 * @param key The key under which the data is stored.
	 * @return The data stored under that key, if any, null otherwise.
	 */
	public String get(String key){
		return data.get(key.toLowerCase());
	}
}