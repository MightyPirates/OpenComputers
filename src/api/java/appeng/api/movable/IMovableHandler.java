package appeng.api.movable;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

public interface IMovableHandler {
	
	/**
	 * if you return true from this, your saying you can handle the class, not that single entity,
	 * you cannot opt out of single entities.
	 * 
	 * @param myClass
	 * @param tile
	 * @return
	 */
	boolean canHandle( Class<? extends TileEntity> myClass, TileEntity tile);
	
	/**
	 * request that the handler move the the tile from its current location to the new one.
	 * the tile has already been invalidated, and the blocks have already been fully moved.
	 * 
	 * Potential Example:
	 * 
		Chunk c = w.getChunkFromBlockCoords( x, z );
		c.setChunkBlockTileEntity( x & 0xF, y + y, z & 0xF, te );
		
        if ( c.isChunkLoaded )
        {
			w.addTileEntity( te );
			w.markBlockForUpdate( x, y, z );
        }
	 * 
	 * @param tile
	 * @param w
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	void moveTile( TileEntity tile, World w, int x, int y, int z );
	
}
