package appeng.api.exceptions;

import cpw.mods.fml.common.FMLLog;
import net.minecraft.world.World;
import appeng.api.DimentionalCoord;

public class AppEngTileMissingException extends Exception {

	private static final long serialVersionUID = -3502227742711078681L;
	public DimentionalCoord dc;
	
	@Override
	public void printStackTrace() {
		try
		{
			FMLLog.info( "[AppEng] Missing Tile at "+dc.x+", "+dc.y+", "+dc.z+" in +"+dc.getWorld().getWorldInfo().getVanillaDimension() );
		}
		catch( Throwable _ )
		{
			FMLLog.info( "[AppEng] Missing Tile at "+dc.x+", "+dc.y+", "+dc.z );
		}
		super.printStackTrace();
	}
	
	public AppEngTileMissingException(World w, int x, int y, int z)
	{
		dc = new DimentionalCoord( w, x, y, z );
	}
	
}
