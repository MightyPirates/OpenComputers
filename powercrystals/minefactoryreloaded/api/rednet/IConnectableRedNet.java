package powercrystals.minefactoryreloaded.api.rednet;

import net.minecraft.world.World;
import net.minecraftforge.common.ForgeDirection;

/**
 * Defines a Block that can connect to RedNet cables. This must be implemented on your Block class.
 * Note that when you implement this, the RedNet network makes several assumptions about your code -
 * primarily, it will not clamp values to 0 <= x <= 15. This means you must be able to accept any
 * possible integer without crashing, even negatives. It will also assume that calling the onInput(s)Changed()
 * methods are sufficient, and will not issue block updates. Finally, it will never call the vanilla redstone
 * output methods in All mode, and will only query the methods contained in this interface in that case. In Single
 * mode, it will call onInputChanged, and will check for strong power (or weak if in Plate mode) through the vanilla
 * method calls.
 * 
 * RedNet cables have their subnets indicated to the user by colored bands on the cable.
 * The color of a given subnet is the same as the wool with metadata equal to the subnet number.
 * For reference:
 * 0:White, 1:Orange, 2:Magenta, 3:LightBlue, 4:Yellow, 5:Lime, 6:Pink, 7:Gray,
 * 8:LightGray, 9:Cyan, 10:Purple, 11:Blue, 12:Brown, 13:Green, 14:Red, 15:Black
 */
public interface IConnectableRedNet
{
	/**
	 * Returns the connection type of this Block. "All" types will cause getOutputValues() and onInputsChanged() to be used,
	 * whereas "Single" types will onInputChanged() to be called for input changes and the normal redstone power output methods
	 * to be called for output. If this value must be changed while the block is alive, it must perform a block update on any
	 * adjacent RedNet wires.
	 * @param world The world this block is in.
	 * @param x This block's X coordinate.
	 * @param y This block's Y coordinate.
	 * @param z This block's Z coordinate.
	 * @param side The side that connection information is required for.
	 * @return The connection type.
	 */
	public RedNetConnectionType getConnectionType(World world, int x, int y, int z, ForgeDirection side);
	
	/**
	 * Returns the output values of this RedNet node. This array must be 16 elements long. Only called if your block is connected in "All" mode.
	 * @param world The world this block is in.
	 * @param x This block's X coordinate.
	 * @param y This block's Y coordinate.
	 * @param z This block's Z coordinate.
	 * @param side The side the output values are required for.
	 * @return The output values.
	 */
	public int[] getOutputValues(World world, int x, int y, int z, ForgeDirection side);
	
	/**
	 * Returns the output value of this RedNet node for a given subnet. Only called if your block is connected in "All" mode.
	 * @param world The world this block is in.
	 * @param x This block's X coordinate.
	 * @param y This block's Y coordinate.
	 * @param z This block's Z coordinate.
	 * @param side The side the output value is required for.
	 * @param subnet The subnet to get the output value for (0-15).
	 * @return The output value.
	 */
	public int getOutputValue(World world, int x, int y, int z, ForgeDirection side, int subnet);
	
	/**
	 * Called when the input values to this block change. Only called if your block is connected in "All" mode.
	 * Do not issue a network value update from inside this method call; it will be ignored. Issue your updates
	 * on the next tick.
	 * @param world The world this block is in.
	 * @param x This block's X coordinate.
	 * @param y This block's Y coordinate.
	 * @param z This block's Z coordinate.
	 * @param side The side the input values are being changed on.
	 * @param inputValues The new set of input values. This array will be 16 elements long.
	 */
	public void onInputsChanged(World world, int x, int y, int z, ForgeDirection side, int[] inputValues);
	
	/**
	 * Called when the input value to this block changes. Only called if your block is connected in "Single" mode.
	 * Do not issue a network value update from inside this method call; it will be ignored. Issue your updates
	 * on the next tick.
	 * @param world The world this block is in.
	 * @param x This block's X coordinate.
	 * @param y This block's Y coordinate.
	 * @param z This block's Z coordinate.
	 * @param side The side the input values are being changed on.
	 * @param inputValue The new input value
	 */
	public void onInputChanged(World world, int x, int y, int z, ForgeDirection side, int inputValue);
}
