package appeng.api;

import net.minecraft.world.World;

public class DimentionalCoord extends WorldCoord
{
	private World w;
	private int dimId;
	
	public DimentionalCoord( DimentionalCoord s )
	{
		super( s.x, s.y, s.z );
		w = s.w;
		dimId = s.dimId;
	}
	
	public DimentionalCoord( World _w, int _x, int _y, int _z )
	{
		super(_x, _y, _z);
		w = _w;
		dimId = _w.provider.dimensionId;
	}
	
	@Override
	public DimentionalCoord copy()
	{
		return new DimentionalCoord( this );
	}

	public boolean isEqual( DimentionalCoord c )
	{
		return x == c.x && y == c.y && z == c.z && c.w == this.w;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if ( obj instanceof DimentionalCoord )
			return isEqual( (DimentionalCoord)obj );
		return false;
	}
	
	@Override
	public int hashCode()
	{
		return super.hashCode() ^ dimId;
	}
	
	public boolean isInWorld(World world)
	{
		return w == world;
	}

	public World getWorld()
	{
		return w;
	}
}
