package appeng.api.events;

import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;
import appeng.api.WorldCoord;
import appeng.api.me.util.IGridInterface;

/**
 * Posted when storage options in a AE Network update, such as a new Storage Bus or Cell is added, or removed.
 */
public class GridStorageUpdateEvent extends AEWorldEvent {
	
	final public WorldCoord coord;
	final public IGridInterface grid;
	
	public GridStorageUpdateEvent(World world, WorldCoord wc, IGridInterface gi ) {
		super(world);
		grid = gi;
		coord = wc;
	}
	
}
