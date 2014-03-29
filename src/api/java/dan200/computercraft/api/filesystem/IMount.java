/**
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2014. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */

package dan200.computercraft.api.filesystem;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Represents a read only part of a virtual filesystem that can be mounted onto a computercraft using IComputerAccess.mount().
 * Ready made implementations of this interface can be created using ComputerCraftAPI.createSaveDirMount() or ComputerCraftAPI.createResourceMount(), or you're free to implement it yourselves!
 * @see dan200.computercraft.api.ComputerCraftAPI#createSaveDirMount(World, String)
 * @see dan200.computercraft.api.ComputerCraftAPI#createResourceMount(Class, String, String)
 * @see dan200.computercraft.api.peripheral.IComputerAccess#mount(String, IMount)
 * @see IWritableMount
 */
public interface IMount
{
	/**
	 * Returns whether a file with a given path exists or not.
	 * @param path A file path in normalised format, relative to the mount location. ie: "programs/myprogram"
	 * @return true if the file exists, false otherwise
	 */
	public boolean exists( String path ) throws IOException;

	/**
	 * Returns whether a file with a given path is a directory or not.
	 * @param path A file path in normalised format, relative to the mount location. ie: "programs/myprograms"
	 * @return true if the file exists and is a directory, false otherwise
	 */
	public boolean isDirectory( String path ) throws IOException;

	/**
	 * Returns the file names of all the files in a directory.
	 * @param path A file path in normalised format, relative to the mount location. ie: "programs/myprograms"
	 * @param contents A list of strings. Add all the file names to this list
	 */
	public void list( String path, List<String> contents ) throws IOException;

	/**
	 * Returns the size of a file with a given path, in bytes
	 * @param path A file path in normalised format, relative to the mount location. ie: "programs/myprogram"
	 * @return the size of the file, in bytes
	 */
	public long getSize( String path ) throws IOException;
	
	/**
	 * Opens a file with a given path, and returns an inputstream representing it's contents.
	 * @param path A file path in normalised format, relative to the mount location. ie: "programs/myprogram"
	 * @return a stream representing the contents of the file
	 */
	public InputStream openForRead( String path ) throws IOException;	
}
