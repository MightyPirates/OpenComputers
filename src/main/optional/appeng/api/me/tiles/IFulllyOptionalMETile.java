package appeng.api.me.tiles;

/**
 * Cables will disconnect if its not enabled,
 * this means that they will fail to appear connected, example - MAC
 */
public interface IFulllyOptionalMETile extends IOptionalMETile {

	boolean isSeperated();
	
}
