package appeng.api.events;

import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;
import appeng.api.WorldCoord;
import appeng.api.me.tiles.IGridTileEntity;

/**
 * A Tile has been added to the world, and should be evaluated for Network Connectivity.
 */
public class GridTileLoadEvent extends AEWorldEvent {
	
	public WorldCoord coord;
	public IGridTileEntity te;
	
	public GridTileLoadEvent(IGridTileEntity _te, World world, WorldCoord wc ) {
		super(world);
		te = _te;
		coord = wc;
	}
	
}
