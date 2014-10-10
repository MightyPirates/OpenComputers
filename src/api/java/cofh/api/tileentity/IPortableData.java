package cofh.api.tileentity;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Implement this interface on Tile Entities which can write a limited amount of data about themselves.
 * 
 * This is typically for the purposes of being transferred to a similar tile entity.
 * 
 * @author King Lemming
 * 
 */
public interface IPortableData {

	/**
	 * Data identifier of the Tile Entity/Block. Used for display as well as verification purposes. Tiles with completely interchangeable data should return the
	 * same type.
	 * 
	 * @return
	 */
	String getDataType();

	/**
	 * Read the data from a tag. The player object exists because this should always be called via player interaction!
	 */
	void readPortableData(EntityPlayer player, NBTTagCompound tag);

	/**
	 * Write the data to a tag. The player object exists because this should always be called via player interaction!
	 */
	void writePortableData(EntityPlayer player, NBTTagCompound tag);
}
