/**
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2013. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */

package dan200.computer.api;
import dan200.computer.api.IPeripheral;

/**
 * A subclass of IPeripheral specifically for peripherals
 * created by ITurtleUpgrade's of type Peripheral. When an
 * IHostedPeripheral is created, its IPeripheral methods will be called
 * just as if the peripheral was a seperate adjacent block in the world,
 * and update() will be called once per tick.
 * @see ITurtleUpgrade
 */
public interface IHostedPeripheral extends IPeripheral
{
	/**
	 * A method called on each hosted peripheral once per tick, on the main thread
	 * over the lifetime of the turtle or block. May be used to update the state 
	 * of the peripheral, and may interact with IComputerAccess or ITurtleAccess
	 * however it likes at this time.
	 */
	public void update();
	
	/**
	 * A method called whenever data is read from the Turtle's NBTTag,
	 * over the lifetime of the turtle. You should only use this for 
	 * reading data you want to stay with the peripheral.
	 * @param nbttagcompound	The peripheral's NBTTag
	 */
	public void readFromNBT( net.minecraft.nbt.NBTTagCompound nbttagcompound );
	
	/**
	 * A method called whenever data is written to the Turtle's NBTTag,
	 * over the lifetime of the turtle. You should only use this for 
	 * writing data you want to stay with the peripheral.
	 * @param nbttagcompound	The peripheral's NBTTag.
	 * @param ID				The turtle's ID.
	 */
	public void writeToNBT( net.minecraft.nbt.NBTTagCompound nbttagcompound );
}
