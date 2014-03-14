package appeng.api.me.tiles;

/**
 * Cables will connect, but signal will not propagate. ( IFulllyOptionalMETile disconnects )
 */
public interface IOptionalMETile {
	
	/**
	 * if false, the signal will not propigate via this tile, remember to use connectivity events.
	 * @return
	 */
	public boolean isEnabled();
	
}
