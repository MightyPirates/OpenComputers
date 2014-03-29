/**
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2014. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */

package dan200.computercraft.api.turtle;

import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Icon;

/**
 * The primary interface for defining an turtle for Turtles. A turtle turtle
 * can either be a new tool, or a new peripheral.
 * @see dan200.computercraft.api.ComputerCraftAPI#registerUpgrade( ITurtleUpgrade )
 */
public interface ITurtleUpgrade
{
	/**
	 * Gets a unique numerical identifier representing this type of turtle turtle.
	 * Like Minecraft common and item IDs, you should strive to make this number unique
	 * among all turtle turtle that have been released for ComputerCraft.
	 * The ID must be in the range 64 to 255, as the ID is stored as an 8-bit value,
	 * and 0-64 is reserved for future use by ComputerCraft. The turtle will
	 * fail registration if an already used ID is specified.
	 * @see dan200.computercraft.api.ComputerCraftAPI#registerUpgrade( ITurtleUpgrade )
	 */
	public int getUpgradeID();
	
	/**
	 * Return a String to describe this type of turtle in turtle item names.
	 * Examples of built-in adjectives are "Wireless", "Mining" and "Crafty".
	 */	
	public String getAdjective();

	/**
	 * Return whether this turtle adds a tool or a peripheral to the turtle.
	 * Currently, turtle crafting is restricted to one tool & one peripheral per turtle.
	 * @see TurtleUpgradeType for the differences between the two.
	 */	
	public TurtleUpgradeType getType();
	
	/**
	 * Return an item stack representing the type of item that a turtle must be crafted
	 * with to create a turtle which holds this turtle.
	 * Currently, turtle crafting is restricted to one tool & one peripheral per turtle.
	 */		
	public ItemStack getCraftingItem();

    /**
	 * Will only be called for Peripheral turtle. Creates a peripheral for a turtle
	 * being placed using this turtle. The peripheral created will be stored
	 * for the lifetime of the turtle, will have update() called once-per-tick, and will be
	 * attach'd detach'd and have methods called in the same manner as a Computer peripheral.
	 *
     * @param turtle Access to the turtle that the peripheral is being created for.
     * @param side Which side of the turtle (left or right) that the turtle resides on.
     * @return The newly created peripheral. You may return null if this turtle is a Tool
	 * and this method is not expected to be called.
	 */		
	public IPeripheral createPeripheral( ITurtleAccess turtle, TurtleSide side );

	/**
	 * Will only be called for Tool turtle. Called when turtle.dig() or turtle.attack() is called
	 * by the turtle, and the tool is required to do some work.
	 * @param turtle Access to the turtle that the tool resides on.
	 * @param side Which side of the turtle (left or right) the tool resides on.
	 * @param verb Which action (dig or attack) the turtle is being called on to perform.
	 * @param direction Which world direction the action should be performed in, relative to the turtles
	 * position. This will either be up, down, or the direction the turtle is facing, depending on
	 * whether dig, digUp or digDown was called.
	 * @return Whether the turtle was able to perform the action, and hence whether the turtle.dig()
	 * or turtle.attack() lua method should return true. If true is returned, the tool will perform
	 * a swinging animation. You may return null if this turtle is a Peripheral
	 * and this method is not expected to be called.
	 */
	public TurtleCommandResult useTool( ITurtleAccess turtle, TurtleSide side, TurtleVerb verb, int direction );

	/**
	 * Called to obtain the Icon to be used when rendering a turtle peripheral. Needs to be a "common"
	 * type Icon for now, as there is no way to determine which texture sheet an Icon is from by the 
	 * Icon itself.
	 * @param turtle Access to the turtle that the peripheral resides on.
	 * @param side Which side of the turtle (left or right) the peripheral resides on.
	 * @return The Icon that you wish to be used to render your turtle peripheral.
	 */
	public Icon getIcon( ITurtleAccess turtle, TurtleSide side );

    /**
     * TODO: Document me
     */
    public void update( ITurtleAccess turtle, TurtleSide side );
}
