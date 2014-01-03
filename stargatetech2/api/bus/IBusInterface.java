package stargatetech2.api.bus;

import net.minecraft.nbt.NBTTagCompound;

/**
 * <b>DO NOT IMPLEMENT THIS INTERFACE!</b> To get an instance use
 * <i>StargateTechAPI.api().getFactory().getIBusInterface()</i>;
 * 
 * @author LordFokas
 */
public interface IBusInterface {
	/**
	 * This method is used to make the network remap it's devices.
	 * Unless you are Sangar from OpenComputers, you probably don't need this.
	 * 
	 * Instead, use the BusEvent, to make StargateTech handle that automatically.
	 * You should use BusEvent.AddToNetwork when the containing block is added and
	 * BusEvent.RemoveFromNetwork when the containing block is removed from the world.
	 */
	public void updateAddressingTable();
	
	/**
	 * Makes the IBusInterface call its IBusDriver's
	 * getNextPacketToSend() method repeatedly until it returns
	 * null. Every packet returned by that method will be sent
	 * across the network.
	 */
	public void sendAllPackets();
	
	/**
	 * Serialize this object.
	 * 
	 * @param nbt The tag compound where this object's data is.
	 * @param tag The name of the tag under which this object's data is stored.
	 */
	public void writeToNBT(NBTTagCompound nbt, String tag);
	
	/**
	 * Unserialize this object.
	 * 
	 * @param nbt The tag compound where this object's data is.
	 * @param tag The name of the tag under which this object's data is stored.
	 */
	public void readFromNBT(NBTTagCompound nbt, String tag);
}