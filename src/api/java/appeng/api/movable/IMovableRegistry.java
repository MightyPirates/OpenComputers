package appeng.api.movable;

import net.minecraft.tileentity.TileEntity;

/**
 * Used to determine if a tile is marked as movable, a block will be considered
 * movable, if...
 * 
 * 1. The Tile or its super classes have been white listed with
 * whiteListTileEntity.
 * 
 * 2. The Tile has been register with the IMC ( which
 * basically calls whiteListTileEntity. )
 *
 * 3. The Tile implements IMovableTile 4.
 * A IMovableHandler is register that returns canHandle = true for the Tile
 * Entity Class
 * 
 * IMC Example: FMLInterModComms.sendMessage( "AppliedEnergistics",
 * "movabletile", "appeng.common.AppEngTile" );
 * 
 * The movement process is as follows,
 * 
 * 1. IMovableTile.prepareToMove() or TileEntity.invalidate() depending on your
 * opt-in method. 2. The tile will be removed from the world. 3. Its world,
 * coordinates will be changed. *** this can be overridden with a
 * IMovableHandler *** 4. It will then be re-added to the world, or a new world.
 * 5. TileEntity.validate() 6. IMovableTile.doneMoving ( if you implemented
 * IMovableTile )
 * 
 * Please note, this is a 100% white list only feature, I will never opt in any
 * non-vanilla, non-AE blocks. If you do not want to support your tiles being
 * moved, you don't have to do anything.
 * 
 * I appreciate anyone that takes the effort to get their tiles to work with
 * this system to create a better use experience.
 * 
 * If you need a build of deobf build of AE for testing, do not hesitate to ask.
 */
public interface IMovableRegistry
{

	/**
	 * White list your tile entity with the registry.
	 * 
	 * You can also use the IMC, FMLInterModComms.sendMessage(
	 * "AppliedEnergistics", "movabletile", "appeng.common.AppEngTile" );
	 * 
	 * If you tile is handled with IMovableHandler or IMovableTile you do not
	 * need to white list it.
	 */
	void whiteListTileEntity(Class<? extends TileEntity> c);

	/**
	 * @param te
	 * @return true if the tile has accepted your request to move it
	 */
	boolean askToMove(TileEntity te);

	/**
	 * tells the tile you are done moving it.
	 * 
	 * @param te
	 */
	void doneMoving(TileEntity te);

	/**
	 * add a new handler movable handler.
	 * 
	 * @param handler
	 */
	void addHandler(IMovableHandler handler);

	/**
	 * handlers are used to perform movement, this allows you to override AE's
	 * internal version.
	 * 
	 * only valid after askToMove(...) = true
	 * 
	 * @param te
	 * @return
	 */
	IMovableHandler getHandler(TileEntity te);

	/**
	 * @return a copy of the default handler
	 */
	IMovableHandler getDefaultHandler();

}