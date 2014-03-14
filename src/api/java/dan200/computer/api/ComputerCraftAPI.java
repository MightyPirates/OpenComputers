/**
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2013. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */

package dan200.computer.api;
import java.lang.reflect.Method;
import net.minecraft.world.World;

/**
 * The static entry point to the ComputerCraft API.
 * Members in this class must be called after mod_ComputerCraft has been initialised,
 * but may be called before it is fully loaded.
 */
public class ComputerCraftAPI 
{	
	/**
	 * Creates a numbered directory in a subfolder of the save directory for a given world, and returns that number.<br>
	 * Use in conjuction with createSaveDirMount() to create a unique place for your peripherals or media items to store files.<br>
	 * @param world The world for which the save dir should be created. This should be the serverside world object.
	 * @param parentSubPath The folder path within the save directory where the new directory should be created. eg: "computer/disk"
	 * @return The numerical value of the name of the new folder, or -1 if the folder could not be created for some reason.<br>
	 * eg: if createUniqueNumberedSaveDir( world, "computer/disk" ) was called returns 42, then "computer/disk/42" is now available for writing.
	 * @see #createSaveDirMount(World, String)
	 */
	public static int createUniqueNumberedSaveDir( World world, String parentSubPath )
	{
		findCC();
		if( computerCraft_createUniqueNumberedSaveDir != null )
		{
			try {
				return ((Integer)computerCraft_createUniqueNumberedSaveDir.invoke( null, world, parentSubPath )).intValue();
			} catch (Exception e){
				// It failed
			}
		}
		return -1;
	}
	
	/**
	 * Creates a file system mount that maps to a subfolder of the save directory for a given world, and returns it.<br>
	 * Use in conjuction with IComputerAccess.mount() or IComputerAccess.mountWritable() to mount a folder from the
	 * users save directory onto a computers file system.<br>
	 * @param world The world for which the save dir can be found. This should be the serverside world object.
	 * @param subPath The folder path within the save directory that the mount should map to. eg: "computer/disk/42".<br>
	 * Use createUniqueNumberedSaveDir() to create a new numbered folder to use.
	 * @param capacity The ammount of data that can be stored in the directory before it fills up, in bytes.
	 * @return The mount, or null if it could be created for some reason. Use IComputerAccess.mount() or IComputerAccess.mountWritable()
	 * to mount this on a Computers' file system.
	 * @see #createUniqueNumberedSaveDir(World, String)
	 * @see IComputerAccess#mount(String, IMount)
	 * @see IComputerAccess#mountWritable(String, IWritableMount)
	 * @see IMount
	 * @see IMountWritable
	 */
	public static IWritableMount createSaveDirMount( World world, String subPath, long capacity )
	{
		findCC();
		if( computerCraft_createSaveDirMount != null )
		{
			try {
				return (IWritableMount)computerCraft_createSaveDirMount.invoke( null, world, subPath, capacity );
			} catch (Exception e){
				// It failed
			}
		}
		return null;
	}
	 
	/**
	 * Creates a file system mount to a resource folder, and returns it.<br>
	 * Use in conjuction with IComputerAccess.mount() or IComputerAccess.mountWritable() to mount a resource folder onto a computers file system.<br>
	 * The files in this mount will be a combination of files in the specified mod jar, and resource packs that contain resources with the same domain and path.<br>
	 * @param class A class in whose jar to look first for the resources to mount. Using your main mod class is recommended. eg: MyMod.class
	 * @param domain The domain under which to look for resources. eg: "mymod"
	 * @param subPath The domain under which to look for resources. eg: "mymod/lua/myfiles"
	 * @return The mount, or null if it could be created for some reason. Use IComputerAccess.mount() or IComputerAccess.mountWritable()
	 * to mount this on a Computers' file system.
	 * @see IComputerAccess#mount(String, IMount)
	 * @see IComputerAccess#mountWritable(String, IMountWritable)
	 * @see IMount
	 */
	public static IMount createResourceMount( Class modClass, String domain, String subPath )
	{
		findCC();
		if( computerCraft_createResourceMount != null )
		{
			try {
				return (IMount)computerCraft_createResourceMount.invoke( null, modClass, domain, subPath );
			} catch (Exception e){
				// It failed
			}
		}
		return null;
	}
	 
	/**
	 * Registers a peripheral handler for a TileEntity that you do not have access to. Only
	 * use this if you want to expose IPeripheral on a TileEntity from another mod. For your own
	 * mod, just implement IPeripheral on the TileEntity directly.
	 * @see IPeripheral
	 * @see IPeripheralHandler
	 */
	public static void registerExternalPeripheral( Class <? extends net.minecraft.tileentity.TileEntity> clazz, IPeripheralHandler handler )
	{
		findCC();
		if (computerCraft_registerExternalPeripheral != null)
		{
			try {
				computerCraft_registerExternalPeripheral.invoke(null, clazz, handler);
			} catch (Exception e){
				// It failed
			}
		}
	}

	// The functions below here are private, and are used to interface with the non-API ComputerCraft classes.
	// Reflection is used here so you can develop your mod in MCP without decompiling ComputerCraft and including
	// it in your solution.
	
	private static void findCC()
	{
		if( !ccSearched ) {
			try {
				computerCraft = Class.forName( "dan200.ComputerCraft" );
				computerCraft_createUniqueNumberedSaveDir = findCCMethod( "createUniqueNumberedSaveDir", new Class[] {
					World.class, String.class
				} );
				computerCraft_createSaveDirMount = findCCMethod( "createSaveDirMount", new Class[] {
					World.class, String.class, Long.TYPE
				} );
				computerCraft_createResourceMount = findCCMethod( "createResourceMount", new Class[] {
					Class.class, String.class, String.class
				} );
				computerCraft_registerExternalPeripheral = findCCMethod( "registerExternalPeripheral", new Class[] { 
					Class.class, IPeripheralHandler.class 
				} );
			} catch( Exception e ) {
				net.minecraft.server.MinecraftServer.getServer().logInfo( "ComputerCraftAPI: ComputerCraft not found." );
			} finally {
				ccSearched = true;
			}
		}
	}

	private static Method findCCMethod( String name, Class[] args )
	{
		try {
			return computerCraft.getMethod( name, args );
		} catch( NoSuchMethodException e ) {
			net.minecraft.server.MinecraftServer.getServer().logInfo( "ComputerCraftAPI: ComputerCraft method " + name + " not found." );
			return null;
		}
	}	
	
	private static boolean ccSearched = false;	
	private static Class computerCraft = null;
	private static Method computerCraft_createUniqueNumberedSaveDir = null;
	private static Method computerCraft_createSaveDirMount = null;
	private static Method computerCraft_createResourceMount = null;
	private static Method computerCraft_registerExternalPeripheral = null;
}
