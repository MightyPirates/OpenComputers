package appeng.api.movable;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public interface IMovableHandler
{

	/**
	 * if you return true from this, your saying you can handle the class, not
	 * that single entity, you cannot opt out of single entities.
	 * 
	 * @param myClass
	 * @param tile
	 * @return
	 */
	boolean canHandle(Class<? extends TileEntity> myClass, TileEntity tile);

	/**
	 * request that the handler move the the tile from its current location to
	 * the new one. the tile has already been invalidated, and the blocks have
	 * already been fully moved.
	 * 
	 * Potential Example:
	 * 
	 * Chunk c = world.getChunkFromBlockCoords( x, z ); c.setChunkBlockTileEntity( x
	 * & 0xF, y + y, z & 0xF, tile );
	 * 
	 * if ( c.isChunkLoaded ) { world.addTileEntity( tile ); world.markBlockForUpdate( x,
	 * y, z ); }
	 * 
	 * @param tile
	 * @param world
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	void moveTile(TileEntity tile, World world, int x, int y, int z);

}