/**
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2014. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */

package dan200.computercraft.api.media;

import dan200.computercraft.api.filesystem.IMount;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * Represents an item that can be placed in a disk drive and used by a Computer.
 * Implement this interface on your Item class to allow it to be used in the drive.
 */
public interface IMedia
{
	/**
	 * Get a string representing the label of this item. Will be called vi disk.getLabel() in lua.
	 * @param stack The itemstack to inspect
	 * @return The label. ie: "Dan's Programs"
	 */
	public String getLabel( ItemStack stack );

	/**
	 * Set a string representing the label of this item. Will be called vi disk.setLabel() in lua.
	 * @param stack The itemstack to modify.
	 * @param label The string to set the label to.
	 * @return true if the label was updated, false if the label may not be modified.
	 */
	public boolean setLabel( ItemStack stack, String label );
	
	/**
	 * If this disk represents an item with audio (like a record), get the readable name of the audio track. ie: "Jonathon Coulton - Still Alive"
	 * @param stack The itemstack to inspect.
	 * @return The name, or null if this item does not represent an item with audio.
	 */
	public String getAudioTitle( ItemStack stack );

	/**
	 * If this disk represents an item with audio (like a record), get the resource name of the audio track to play.
	 * @param stack The itemstack to inspect.
	 * @return The name, or null if this item does not represent an item with audio.
	 */
	public String getAudioRecordName( ItemStack stack );	
    
	/**
	 * If this disk represents an item with data (like a floppy disk), get a mount representing it's contents. This will be mounted onto the filesystem of the computercraft while the media is in the disk drive.
	 * @param stack The itemstack to inspect.
	 * @param world The world in which the item and disk drive reside.
	 * @return The mount, or null if this item does not represent an item with data. If the IMount returned also implements IWritableMount, it will mounted using mountWritable()
	 * @see dan200.computercraft.api.filesystem.IMount
	 * @see dan200.computercraft.api.filesystem.IWritableMount
	 * @see dan200.computercraft.api.ComputerCraftAPI#createSaveDirMount(World, String, long)
	 * @see dan200.computercraft.api.ComputerCraftAPI#createResourceMount(Class, String, String)
	 */
    public IMount createDataMount( ItemStack stack, World world );
}
