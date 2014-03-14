package appeng.api.events;

import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;
import appeng.api.WorldCoord;
import appeng.api.me.tiles.IGridTileEntity;

/**
 * The network has changed, and must be updated.
 */
public class GridTileConnectivityEvent extends AEWorldEvent {
	
	public WorldCoord coord;
	public IGridTileEntity te;
	
	public GridTileConnectivityEvent( IGridTileEntity _te, World world, WorldCoord wc ) {
		super(world);
		te = _te;
		coord = wc;
	}
	
}
