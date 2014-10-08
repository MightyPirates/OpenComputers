package cofh.api.tileentity;

/**
 * Implement this interface on Tile Entities which cache their redstone status.
 * 
 * Note that {@link IRedstoneControl} is an extension of this.
 * 
 * @author King Lemming
 * 
 */
public interface IRedstoneCache {

	void setPowered(boolean isPowered);

	boolean isPowered();

}
