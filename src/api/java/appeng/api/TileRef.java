package appeng.api;

import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import appeng.api.events.GridTileUnloadEvent;
import appeng.api.exceptions.AppEngTileMissingException;
import appeng.api.me.tiles.IGridTileEntity;

public class TileRef<T> extends WorldCoord {
	
	//private int dimension;
	private World w;
	private Class myType;
	
	boolean wasGrid;
	
	public TileRef( TileEntity gte ) {
		super( gte.xCoord, gte.yCoord, gte.zCoord );
		TileEntity te = gte;
		myType = gte.getClass();
		wasGrid = te instanceof IGridTileEntity;
		w = te.worldObj;
		if ( te.worldObj == null )
			throw new RuntimeException("Tile has no world.");
	}
	
	@SuppressWarnings("unchecked")
	public T getTile() throws AppEngTileMissingException
	{
		// there might be a possible tick where we have TileRefs for unloaded tiles?
		if ( w.getChunkProvider().chunkExists(x >> 4, z >> 4) )
		{
			TileEntity te = w.getBlockTileEntity( x, y, z );
			if ( te != null && myType.isInstance( te ) )
				return (T)te;
		}
		
		/**
		 * was this a grid tile? if so inform the grid enum that something has derped.
		 */
		if ( wasGrid )
		{
			wasGrid = false; // no need to keep this up..
			MinecraftForge.EVENT_BUS.post( new GridTileUnloadEvent( null, w, this ) );
		}
		
		throw new AppEngTileMissingException( w, x,y,z);
	}

	public DimentionalCoord getCoord() {
		return new DimentionalCoord( w, x, y, z );
	}
	
	@Override
	public boolean equals(Object obj) {
		
		// is it the same?
		if ( obj instanceof TileEntity )
		{
			TileEntity te = (TileEntity) obj;
			return w == te.worldObj && te.xCoord == x && te.yCoord == y && te.zCoord == z;
		}
		
		return super.equals( obj );
	}
	
};
