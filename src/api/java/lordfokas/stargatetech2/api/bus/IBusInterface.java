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