package appeng.api.events;

import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;
import appeng.api.WorldCoord;

/**
 * an error, basically just causes an update, use it if there is really an error, other wise just ignore it.
 */
public class GridErrorEvent extends AEWorldEvent {
	
	public WorldCoord coord;
	public GridErrorEvent(World world, WorldCoord wc ) {
		super(world);
		coord = wc;
	}
	
}
