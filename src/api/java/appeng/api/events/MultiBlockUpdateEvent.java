package appeng.api.events;

import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;
import appeng.api.WorldCoord;
import appeng.api.me.tiles.IGridTileEntity;

/**
 * Used by the MAC to trigger updates in its structure.
 */
public class MultiBlockUpdateEvent extends AEWorldEvent {
	
	public WorldCoord coord;
	public IGridTileEntity te;
	
	public MultiBlockUpdateEvent( IGridTileEntity _te, World world, WorldCoord wc ) {
		super(world);
		coord = wc;
		te = _te;
	}
	
}
