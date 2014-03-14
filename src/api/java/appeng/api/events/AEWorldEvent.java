package appeng.api.events;

import net.minecraft.world.World;
import net.minecraftforge.event.Event;

/**
 * Simple Alternative to WorldEvent, thanks to a certain mod that wants to crash on WorldEvents
 */
public class AEWorldEvent extends Event
{
	final public World world;
	
	public AEWorldEvent( World w )
	{
		world = w;
	}
}
