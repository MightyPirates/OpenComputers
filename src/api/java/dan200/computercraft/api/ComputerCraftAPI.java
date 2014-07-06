/**
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2014. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */

package dan200.computercraft.api;

import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.api.filesystem.IWritableMount;
import dan200.computercraft.api.media.IMediaProvider;
import dan200.computercraft.api.peripheral.IPeripheralProvider;
import dan200.computercraft.api.redstone.IBundledRedstoneProvider;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import net.minecraft.world.World;

import java.lang.reflect.Method;

/**
 * The static entry point to the ComputerCraft API.
 * Members in this class must be called after mod_ComputerCraft has been initialised,
 * but may be called before it is fully loaded.
 */
public final class ComputerCraftAPI
{	
	/**
	 * Creates a numbered directory in a subfolder of the save directory for a given world, and returns that number.<br>
	 * Use in conjuction with createSaveDirMount() to create a unique place for your peripherals or media items to store files.<br>
	 * @param world The world for which the save dir should be created. This should be the serverside world object.
	 * @param parentSubPath The folder path within the save directory where the new directory should be created. eg: "computercraft/disk"
	 * @return The numerical value of the name of the new folder, or -1 if the folder could not be created for some reason.<br>
	 * eg: if createUniqueNumberedSaveDir( world, "computer/disk" ) was called returns 42, then "computer/disk/42" is now available for writing.
	 * @see #createSaveDirMount(World, String, long)
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
	 * @see dan200.computercraft.api.peripheral.IComputerAccess#mount(String, dan200.computercraft.api.filesystem.IMount)
	 * @see dan200.computercraft.api.peripheral.IComputerAccess#mountWritable(String, dan200.computercraft.api.filesystem.IWritableMount)
	 * @see dan200.computercraft.api.filesystem.IMount
	 * @see IWritableMount
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
	 * @param modClass A class in whose jar to look first for the resources to mount. Using your main mod class is recommended. eg: MyMod.class
	 * @param domain The domain under which to look for resources. eg: "mymod"
	 * @param subPath The domain under which to look for resources. eg: "mymod/lua/myfiles"
	 * @return The mount, or null if it could be created for some reason. Use IComputerAccess.mount() or IComputerAccess.mountWritable()
	 * to mount this on a Computers' file system.
	 * @see dan200.computercraft.api.peripheral.IComputerAccess#mount(String, dan200.computercraft.api.filesystem.IMount)
	 * @see dan200.computercraft.api.peripheral.IComputerAccess#mountWritable(String, IWritableMount)
	 * @see dan200.computercraft.api.filesystem.IMount
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
	 * Registers a peripheral handler to convert blocks into IPeripheral implementations.
	 * @see dan200.computercraft.api.peripheral.IPeripheral
	 * @see dan200.computercraft.api.peripheral.IPeripheralProvider
	 */
	public static void registerPeripheralProvider( IPeripheralProvider handler )
	{
		findCC();
		if ( computerCraft_registerPeripheralProvider != null)
		{
			try {
				computerCraft_registerPeripheralProvider.invoke( null, handler );
			} catch (Exception e){
				// It failed
			}
		}
	}

    /**
     * Registers a new turtle turtle for use in ComputerCraft. After calling this,
     * users should be able to craft Turtles with your new turtle. It is recommended to call
     * this during the load() method of your mod.
     * @see dan200.computercraft.api.turtle.ITurtleUpgrade
     */
    public static void registerTurtleUpgrade( ITurtleUpgrade upgrade )
    {
        if( upgrade != null )
        {
            findCC();
            if( computerCraft_registerTurtleUpgrade != null )
            {
                try {
                    computerCraft_registerTurtleUpgrade.invoke( null, upgrade );
                } catch( Exception e ) {
                    // It failed
                }
            }
        }
    }

    /**
     * Registers a bundled redstone handler to provide bundled redstone output for blocks
     * @see dan200.computercraft.api.redstone.IBundledRedstoneProvider
     */
    public static void registerBundledRedstoneProvider( IBundledRedstoneProvider handler )
    {
        findCC();
        if( computerCraft_registerBundledRedstoneProvider != null )
        {
            try {
                computerCraft_registerBundledRedstoneProvider.invoke( null, handler );
            } catch (Exception e) {
                // It failed
            }
        }
    }

    /**
     * If there is a Computer or Turtle at a certain position in the world, get it's bundled redstone output.
     * @see dan200.computercraft.api.redstone.IBundledRedstoneProvider
     * @return If there is a block capable of emitting bundled redstone at the location, it's signal (0-65535) will be returned.
     * If there is no block capable of emitting bundled redstone at the location, -1 will be returned.
     */
    public static int getBundledRedstoneOutput( World world, int x, int y, int z, int side )
    {
        findCC();
        if( computerCraft_getDefaultBundledRedstoneOutput != null )
        {
            try {
                return ((Integer)computerCraft_getDefaultBundledRedstoneOutput.invoke( null, world, x, y, z, side )).intValue();
            } catch (Exception e){
                // It failed
            }
        }
        return -1;
    }

    /**
     * Registers a media handler to provide IMedia implementations for Items
     * @see dan200.computercraft.api.media.IMediaProvider
     */
    public static void registerMediaProvider( IMediaProvider handler )
    {
        findCC();
        if( computerCraft_registerMediaProvider != null )
        {
            try {
                computerCraft_registerMediaProvider.invoke( null, handler );
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
				computerCraft = Class.forName( "dan200.computercraft.ComputerCraft" );
				computerCraft_createUniqueNumberedSaveDir = findCCMethod( "createUniqueNumberedSaveDir", new Class[]{
                    World.class, String.class
                } );
				computerCraft_createSaveDirMount = findCCMethod( "createSaveDirMount", new Class[] {
					World.class, String.class, Long.TYPE
				} );
				computerCraft_createResourceMount = findCCMethod( "createResourceMount", new Class[] {
					Class.class, String.class, String.class
				} );
				computerCraft_registerPeripheralProvider = findCCMethod( "registerPeripheralProvider", new Class[] {
					IPeripheralProvider.class
				} );
                computerCraft_registerTurtleUpgrade = findCCMethod( "registerTurtleUpgrade", new Class[] {
                    ITurtleUpgrade.class
                } );
                computerCraft_registerBundledRedstoneProvider = findCCMethod( "registerBundledRedstoneProvider", new Class[] {
                    IBundledRedstoneProvider.class
                } );
                computerCraft_getDefaultBundledRedstoneOutput = findCCMethod( "getDefaultBundledRedstoneOutput", new Class[] {
                    World.class, Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE
                } );
                computerCraft_registerMediaProvider = findCCMethod( "registerMediaProvider", new Class[] {
                    IMediaProvider.class
                } );
			} catch( Exception e ) {
				System.out.println( "ComputerCraftAPI: ComputerCraft not found." );
			} finally {
				ccSearched = true;
			}
		}
	}

	private static Method findCCMethod( String name, Class[] args )
	{
		try {
            if( computerCraft != null )
            {
    			return computerCraft.getMethod( name, args );
            }
            return null;
		} catch( NoSuchMethodException e ) {
			System.out.println( "ComputerCraftAPI: ComputerCraft method " + name + " not found." );
			return null;
		}
	}	
	
	private static boolean ccSearched = false;	
	private static Class computerCraft = null;
	private static Method computerCraft_createUniqueNumberedSaveDir = null;
	private static Method computerCraft_createSaveDirMount = null;
	private static Method computerCraft_createResourceMount = null;
	private static Method computerCraft_registerPeripheralProvider = null;
    private static Method computerCraft_registerTurtleUpgrade = null;
    private static Method computerCraft_registerBundledRedstoneProvider = null;
    private static Method computerCraft_getDefaultBundledRedstoneOutput = null;
    private static Method computerCraft_registerMediaProvider = null;
}
