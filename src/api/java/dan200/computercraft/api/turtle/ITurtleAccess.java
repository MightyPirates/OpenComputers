/**
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2014. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */

package dan200.computercraft.api.turtle;

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.inventory.IInventory;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

/**
 * The interface passed to turtle by turtles, providing methods that they can call.
 * This should not be implemented by your classes. Do not interact with turtles except via this interface and ITurtleUpgrade.
 */
public interface ITurtleAccess
{
	/**
	 * Returns the world in which the turtle resides.
	 * @return the world in which the turtle resides.
	 */
	public World getWorld();

	/**
	 * Returns a vector containing the integer co-ordinates at which the turtle resides.
	 * @return a vector containing the integer co-ordinates at which the turtle resides.
	 */
	public ChunkCoordinates getPosition();

    /**
     * TODO: Document me
     */
    public boolean teleportTo( World world, int x, int y, int z );

	/**
	 * Returns a vector containing the floating point co-ordinates at which the turtle is rendered.
	 * This will shift when the turtle is moving.
	 * @param f The subframe fraction
	 * @return a vector containing the floating point co-ordinates at which the turtle resides.
	 */
	public Vec3 getVisualPosition( float f );

    /**
     * TODO: Document me
     */
    public float getVisualYaw( float f );

	/**
	 * Returns the world direction the turtle is currently facing.
	 * @return the world direction the turtle is currently facing.
	 */
	public int getDirection();

    /**
     * TODO: Document me
     */
    public void setDirection( int dir );

    /**
     * TODO: Document me
     */
	public int getSelectedSlot();

    /**
     * TODO: Document me
     */
    public void setSelectedSlot( int slot );

    /**
     * TODO: Document me
     */
    public IInventory getInventory();

    /**
     * TODO: Document me
     */
    public boolean isFuelNeeded();

	/**
	 * TODO: Document me
	 */
	public int getFuelLevel();

    /**
     * TODO: Document me
     */
    public void setFuelLevel( int fuel );

    /**
     * TODO: Document me
     */
    public int getFuelLimit();

    /**
	 * Removes some fuel from the turtles fuel supply. Negative numbers can be passed in to INCREASE the fuel level of the turtle.
	 * @return Whether the turtle was able to consume the ammount of fuel specified. Will return false if you supply a number
	 * greater than the current fuel level of the turtle.
	 */
	public boolean consumeFuel( int fuel );

    /**
     * TODO: Document me
     */
    public void addFuel( int fuel );

    /**
     * Adds a custom command to the turtles command queue. Unlike peripheral methods, these custom commands will be executed
     * on the main thread, so are guaranteed to be able to access Minecraft objects safely, and will be queued up
     * with the turtles standard movement and tool commands. An issued command will return an unique integer, which will
     * be supplied as a parameter to a "turtle_response" event issued to the turtle after the command has completed. Look at the
     * lua source code for "rom/apis/turtle" for how to build a lua wrapper around this functionality.
     * @param command an object which will execute the custom command when its point in the queue is reached
     * @return the objects the command returned when executed. you should probably return these to the player
     * unchanged if called from a peripheral method.
     * @see ITurtleCommand
     */
    public Object[] executeCommand( ILuaContext context, ITurtleCommand command ) throws Exception;

    /**
     * TODO: Document me
     */
    public void playAnimation( TurtleAnimation animation );

	/**
	 * Returns the turtle on the specified side of the turtle, if there is one.
	 * @return the turtle on the specified side of the turtle, if there is one.
	 */
	public ITurtleUpgrade getUpgrade( TurtleSide side );

    /**
     * TODO: Document me
     */
    public void setUpgrade( TurtleSide side, ITurtleUpgrade upgrade );

	/**
	 * Returns the peripheral created by the upgrade on the specified side of the turtle, if there is one.
	 * @return the peripheral created by the upgrade on the specified side of the turtle, if there is one.
	 */
	public IPeripheral getPeripheral( TurtleSide side );

    /**
     * TODO: Document me
     */
    public NBTTagCompound getUpgradeNBTData( TurtleSide side );

    /**
     * TODO: Document me
     */
    public void updateUpgradeNBTData( TurtleSide side );
}
