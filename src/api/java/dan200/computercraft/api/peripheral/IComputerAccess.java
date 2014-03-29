/**
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2014. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */

package dan200.computercraft.api.peripheral;

import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.api.filesystem.IWritableMount;

/**
 * The interface passed to peripherals by computers or turtles, providing methods
 * that they can call. This should not be implemented by your classes. Do not interact
 * with computers except via this interface.
 */
public interface IComputerAccess
{
	/**
	 * Mount a mount onto the computers' file system in a read only mode.<br>
	 * @param desiredLocation The location on the computercraft's file system where you would like the mount to be mounted.
	 * @param mount The mount object to mount on the computercraft. These can be obtained by calling ComputerCraftAPI.createSaveDirMount(), ComputerCraftAPI.createResourceMount() or by creating your own objects that implement the IMount interface.
	 * @return The location on the computercraft's file system where you the mount mounted, or null if there was already a file in the desired location. Store this value if you wish to unmount the mount later.
	 * @see dan200.computercraft.api.ComputerCraftAPI#createSaveDirMount(World, String)
	 * @see dan200.computercraft.api.ComputerCraftAPI#createResourceMount(Class, String, String)
	 * @see #mountWritable(String, dan200.computercraft.api.filesystem.IWritableMount)
	 * @see #unmount(String)
	 * @see dan200.computercraft.api.filesystem.IMount
	 */
	public String mount( String desiredLocation, IMount mount );
	
	/**
	 * Mount a mount onto the computers' file system in a writable mode.<br>
	 * @param desiredLocation The location on the computercraft's file system where you would like the mount to be mounted.
	 * @param mount The mount object to mount on the computercraft. These can be obtained by calling ComputerCraftAPI.createSaveDirMount() or by creating your own objects that implement the IWritableMount interface.
	 * @return The location on the computercraft's file system where you the mount mounted, or null if there was already a file in the desired location. Store this value if you wish to unmount the mount later.
	 * @see dan200.computercraft.api.ComputerCraftAPI#createSaveDirMount(World, String)
	 * @see dan200.computercraft.api.ComputerCraftAPI#createResourceMount(Class, String, String)
	 * @see #mount(String, IMount)
	 * @see #unmount(String)
	 * @see IMount
	 */
	public String mountWritable( String desiredLocation, IWritableMount mount );
	
	/**
	 * Unmounts a directory previously mounted onto the computers file system by mount() or mountWritable().<br>
	 * When a directory is unmounted, it will disappear from the computers file system, and the user will no longer be able to
	 * access it. All directories mounted by a mount or mountWritable are automatically unmounted when the peripheral
	 * is attached if they have not been explicitly unmounted.
	 * @param location	The desired location in the computers file system of the directory to unmount.
	 *					This must be the location of a directory previously mounted by mount() or mountWritable(), as
	 *					indicated by their return value.
	 * @see	#mount(String, IMount)
	 * @see	#mountWritable(String, IWritableMount)
	 */
	public void unmount( String location );
	
	/**
	 * Returns the numerical ID of this computercraft.<br>
	 * This is the same number obtained by calling os.getComputerID() or running the "id" program from lua,
	 * and is guarunteed unique. This number will be positive.
	 * @return	The identifier.
	 */
	public int getID();	

	/**
	 * Causes an event to be raised on this computercraft, which the computercraft can respond to by calling
	 * os.pullEvent(). This can be used to notify the computercraft when things happen in the world or to
	 * this peripheral.
	 * @param event		A string identifying the type of event that has occurred, this will be
	 *					returned as the first value from os.pullEvent(). It is recommended that you
	 *					you choose a name that is unique, and recognisable as originating from your 
	 *					peripheral. eg: If your peripheral type is "button", a suitable event would be
	 *					"button_pressed".
	 * @param arguments	In addition to a name, you may pass an array of extra arguments to the event, that will
	 *					be supplied as extra return values to os.pullEvent(). Objects in the array will be converted
	 *					to lua data types in the same fashion as the return values of IPeripheral.callMethod().<br>
	 *					You may supply null to indicate that no arguments are to be supplied.
	 * @see dan200.computercraft.api.peripheral.IPeripheral#callMethod
	 */
	public void queueEvent( String event, Object[] arguments );

	/**
	 * Get a string, unique to the computercraft, by which the computercraft refers to this peripheral.
	 * For directly attached peripherals this will be "left","right","front","back",etc, but
	 * for peripherals attached remotely it will be different. It is good practice to supply
	 * this string when raising events to the computercraft, so that the computercraft knows from
	 * which peripheral the event came.
	 * @return A string unique to the computercraft, but not globally.
	 */
	public String getAttachmentName();
}
