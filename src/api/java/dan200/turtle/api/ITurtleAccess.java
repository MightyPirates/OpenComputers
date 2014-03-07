/**
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2013. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */

package dan200.turtle.api;
import dan200.computer.api.*;

/**
 * The interface passed to upgrades by turtles, providing methods that they can call.
 * This should not be implemented by your classes. Do not interact with turtles except via this interface and ITurtleUpgrade.
 */
public interface ITurtleAccess
{
	/**
	 * Returns the world in which the turtle resides.
	 * @return the world in which the turtle resides.
	 */
	public net.minecraft.world.World getWorld();

	/**
	 * Returns a vector containing the integer block co-ordinates at which the turtle resides.
	 * @return a vector containing the integer block co-ordinates at which the turtle resides.
	 */
	public net.minecraft.util.Vec3 getPosition();

	/**
	 * Returns a vector containing the co-ordinates at which the turtle is rendered.
	 * This will shift when the turtle is moving.
	 * @param f The subframe fraction
	 * @return a vector containing the integer block co-ordinates at which the turtle resides.
	 */
	public net.minecraft.util.Vec3 getVisualPosition( float f );

	/**
	 * Returns the world direction the turtle is currently facing.
	 * @return the world direction the turtle is currently facing.
	 */
	public int getFacingDir();
	
	/**
	 * Returns the size of the turtles inventory, in number of slots. This will currently always be 16.
	 * @return the size of the turtles inventory, in number of slots. This will currently always be 16.
	 */
	public int getInventorySize();

	/**
	 * Returns which slot the turtle currently has selected in its inventory using turtle.select().
	 * Unlike the 1-based lua representation, this will be between 0 and getInventorySize() - 1.
	 * @return which slot the turtle currently has selected in its inventory
	 */
	public int getSelectedSlot();
	
	/**
	 * Returns the item stack that the turtle has in one of its inventory slots.
	 * @param index which inventory slot to retreive, should be between 0 and getInventorySize() - 1
	 * @return the item stack that the turtle has in one of its inventory slots. May be null.
	 */
	public net.minecraft.item.ItemStack getSlotContents( int index );

	/**
	 * Changes the item stack that the turtle has in one of its inventory slots.
	 * @param index which inventory slot to change, should be between 0 and getInventorySize() - 1
	 * @param stack an item stack to put in the slot. May be null.
	 */
	public void setSlotContents( int index, net.minecraft.item.ItemStack stack );
	
	/**
	 * Tries to store an item stack into the turtles current inventory, starting from the turtles
	 * currently selected inventory slot.
	 * @param stack The item stack to try and store.
	 * @return true if the stack was completely stored in the inventory, false if 
	 * it was only partially stored, or could not fit at all. If false is returned
	 * and the stack was partially stored, the ItemStack passed into "stack" will now
	 * represent the stack of items that is left over.
	 */
	public boolean storeItemStack( net.minecraft.item.ItemStack stack );

	/**
	 * Drops an item stack from the turtle onto the floor, or into an inventory is there is one
	 * adjacent to the turtle in the direction specified.
	 * @param stack The item stack to drop.
	 * @param dir The world direction to drop the item
	 * @return true if the stack was dropped, or completely stored in the adjacent inventory, false if 
	 * it was only partially stored in the adjacent inventory, or could not fit at all. If false is returned
	 * and the stack was partially stored, the ItemStack passed into "stack" will now
	 * represent the stack of items that is left over.
	 */
	public boolean dropItemStack( net.minecraft.item.ItemStack stack, int dir );
	
	/**
	 * "Deploys" an item stack in the direction specified. This simulates a player right clicking, and calls onItemUse() on the Item class.
	 * Will return true if some kind of deployment happened, and may modify the item stack. For block item types, this can be
	 * used to place blocks. Some kinds of items (such as shears when facing a sheep) may modify the turtles inventory during this call.
	 * @param stack The item stack to deploy
	 * @param dir The world direction to deploy the item
	 * @return true if the stack was deployed, false if it was not.
	 */
	public boolean deployWithItemStack( net.minecraft.item.ItemStack stack, int dir );

	/**
	 * Tries to "attack" entities with an item stack in the direction specified. This simulates a player left clicking, but will
	 * not affect blocks. If an entity is attacked and killed during this call, its dropped items will end up in the turtles
	 * inventory.
	 * @param stack The item stack to attack with
	 * @param dir The world direction to attack with the item
	 * @return true if something was attacked, false if it was not
	 */
	public boolean attackWithItemStack( net.minecraft.item.ItemStack stack, int dir, float damageMultiplier );

	/**
	 * Returns the current fuel level of the turtle, this is the same integer returned by turtle.getFuelLevel(),
	 * that decreases by 1 every time the turtle moves. Can be used to have your tool or peripheral require or supply 
	 * fuel to the turtle.
	 * @return the fuel level
	 */
	public int getFuelLevel();

	/**
	 * Tries to increase the fuel level of a turtle by burning an item stack. If the item passed in is a fuel source, fuel
	 * will increase and true will be returned. Otherwise, nothing will happen and false will be returned.
	 * @param stack The stack to try to refuel with
	 * @return Whether the turtle was refueled
	 */
	public boolean refuelWithItemStack( net.minecraft.item.ItemStack stack );
	
	/**
	 * Removes some fuel from the turtles fuel supply. Negative numbers can be passed in to INCREASE the fuel level of the turtle.
	 * @return Whether the turtle was able to consume the ammount of fuel specified. Will return false if you supply a number
	 * greater than the current fuel level of the turtle.
	 */
	public boolean consumeFuel( int fuel );
	
	/**
	 * Adds a custom command to the turtles command queue. Unlike peripheral methods, these custom commands will be executed
	 * on the main thread, so are guaranteed to be able to access Minecraft objects safely, and will be queued up
	 * with the turtles standard movement and tool commands. An issued command will return an unique integer, which will
	 * be supplied as a parameter to a "turtle_response" event issued to the turtle after the command has completed. Look at the 
	 * lua source code for "rom/apis/turtle" for how to build a lua wrapper around this functionality.
	 * @param handler an object which will execute the custom command when its point in the queue is reached
	 * @return the unique command identifier described above
	 * @see ITurtleCommandHandler
	 */
	public int issueCommand( ITurtleCommandHandler handler );
	
	/**
	 * Returns the upgrade on the specified side of the turtle, if there is one.
	 * @return the upgrade on the specified side of the turtle, if there is one.
	 */
	public ITurtleUpgrade getUpgrade( TurtleSide side );

	/**
	 * Returns the peripheral created by the upgrade on the specified side of the turtle, if there is one.
	 * @return the peripheral created by the upgrade on the specified side of the turtle, if there is one.
	 */
	public IHostedPeripheral getPeripheral( TurtleSide side );
}
