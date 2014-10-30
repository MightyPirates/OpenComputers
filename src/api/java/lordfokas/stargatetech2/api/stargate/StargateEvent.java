package lordfokas.stargatetech2.api.stargate;

import cpw.mods.fml.common.eventhandler.Cancelable;
import cpw.mods.fml.common.eventhandler.Event;
import net.minecraft.world.World;

public abstract class StargateEvent extends Event {

	private static abstract class PhysicalGateEvent extends StargateEvent {
		public final Address address;
		public final World world;
		public final int xCoord;
		public final int yCoord;
		public final int zCoord;
		
		public PhysicalGateEvent(Address addr, World world, int x, int y, int z) {
			this.xCoord = x;
			this.yCoord = y;
			this.zCoord = z;
			this.world = world;
			this.address = addr;
		}
	}
	
	@Cancelable
	public static class StargateWrenched extends PhysicalGateEvent {
		public StargateWrenched(Address addr, World world, int x, int y, int z) {
			super(addr, world, x, y, z);
		}
	}
	
	public static class StargateDestroyed extends PhysicalGateEvent {
		public StargateDestroyed(Address addr, World world, int x, int y, int z) {
			super(addr, world, x, y, z);
		}
	}
	
}
