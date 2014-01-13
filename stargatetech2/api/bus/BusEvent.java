package stargatetech2.api.bus;

import net.minecraft.world.World;
import net.minecraftforge.event.Event;

public class BusEvent extends Event{
	public final World world;
	public final int x, y, z;
	
	protected BusEvent(World world, int x, int y, int z){
		this.world = world;
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	/**
	 * Fire this event on Forge's BUS_EVENT to add the IBusDevice
	 * in this location to a bus network, if any is available.
	 * 
	 * @author LordFokas
	 */
	public static final class AddToNetwork extends BusEvent{
		public AddToNetwork(World world, int x, int y, int z) {
			super(world, x, y, z);
		}
	}
	
	/**
	 * Fire this event on Forge's BUS_EVENT to remove the IBusDevice
	 * in this location from any connected bus networks.
	 * 
	 * @author LordFokas
	 */
	public static final class RemoveFromNetwork extends BusEvent{
		public RemoveFromNetwork(World world, int x, int y, int z) {
			super(world, x, y, z);
		}
	}
}