package stargatetech2.api.bus;

import java.util.LinkedList;

public final class BusPacketNetScan extends BusPacket<Void> {
	private LinkedList<Device> devices = new LinkedList();
	
	public BusPacketNetScan(short target) {
		super((short)0xFFFF, target, false);
	}
	
	// We're not using this
	@Override protected void fillPlainText(BusPacketLIP lip){}
	
	public void addDevice(Device device){
		devices.add(device);
	}
	
	public LinkedList<Device> getDevices(){
		return new LinkedList<Device>(devices);
	}
	
	public static final class Device{
		public final String description, name;
		public final short address;
		public final boolean enabled;
		public final int x, y, z;
		
		public Device(String desc, String name, short address, boolean enabled, int x, int y, int z){
			this.description = desc;
			this.name = name;
			this.address = address;
			this.enabled = enabled;
			this.x = x;
			this.y = y;
			this.z = z;
		}
	}
}