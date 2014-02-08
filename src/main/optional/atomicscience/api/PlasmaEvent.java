package atomicscience.api;

import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;

public abstract class PlasmaEvent extends WorldEvent
{
	public PlasmaEvent(World world)
	{
		super(world);
	}

	/**
	 * Post this event to spawn a plasma block.
	 * This even if fired whenever plasma is called to spawn.
	 * 
	 * @author Calclavia
	 * 
	 */
	public static class SpawnPlasmaEvent extends PlasmaEvent
	{
		public final int x, y, z;
		public final int temperature;

		public SpawnPlasmaEvent(World world, int x, int y, int z, int temperature)
		{
			super(world);
			this.x = x;
			this.y = y;
			this.z = z;
			this.temperature = temperature;
		}
	}
}
