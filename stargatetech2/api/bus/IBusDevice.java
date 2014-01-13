package stargatetech2.api.bus;

import net.minecraft.world.World;

/**
 * To be implemented by Tile Entities that wish
 * to access the Abstract Bus.
 * 
 * @author LordFokas
 */
public interface IBusDevice {
	/**
	 * Returns the IBusInterfaces that exist on that
	 * side of the Tile Entity. It may be multiple
	 * values or null.
	 * 
	 * @param side The side of the block that is being queried.
	 * @return This side's IBusInterface, if any.
	 */
	public IBusInterface[] getInterfaces(int side);
	
	/**
	 * @return This device's worldObj.
	 */
	public World getWorld();
	
	/**
	 * @return This device's X Coordinate.
	 */
	public int getXCoord();
	
	/**
	 * @return This device's Y Coordinate.
	 */
	public int getYCoord();
	
	/**
	 * @return This device's Z Coordinate.
	 */
	public int getZCoord();
}
