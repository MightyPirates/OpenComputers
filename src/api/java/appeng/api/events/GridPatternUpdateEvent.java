package appeng.api.events;

import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;
import appeng.api.WorldCoord;
import appeng.api.me.util.IGridInterface;

/**
 * Posted when crafting options in a AE Network update, you can watch for them if you want to.
 */
public class GridPatternUpdateEvent extends AEWorldEvent {
	
	final public WorldCoord coord;
	final public IGridInterface grid;
	
	public GridPatternUpdateEvent(World world, WorldCoord wc, IGridInterface gi ) {
		super(world);
		grid = gi;
		coord = wc;
	}
	
}
