package appeng.api.events;

import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;
import appeng.api.WorldCoord;
import appeng.api.me.tiles.IGridTileEntity;

/**
 * A Tile has been removed from the world, and should no longer be considered for connectivity.
 */
public class GridTileUnloadEvent extends AEWorldEvent {
	
	public WorldCoord coord;
	public IGridTileEntity te;
	
	public GridTileUnloadEvent(IGridTileEntity _te,World world, WorldCoord wc ) {
		super(world);
		te = _te;
		coord = wc;
	}
	
}
