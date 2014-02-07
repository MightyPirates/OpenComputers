/**
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2013. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */

package dan200.turtle.api;

/**
 * An enum representing the two different types of upgrades that an ITurtleUpgrade
 * implementation can add to a turtle.
 * @see ITurtleUpgrade
 */
public enum TurtleUpgradeType
{
	/**
	 * A tool is rendered as an item on the side of the turtle, and responds to the turtle.dig()
	 * and turtle.attack() methods (Such as pickaxe or sword on Mining and Melee turtles).
	 */
	Tool,
	
	/**
	 * A peripheral adds a special peripheral which is attached to the side of the turtle,
	 * and can be interacted with the peripheral API (Such as the modem on Wireless Turtles).
	 */
	Peripheral,
}
