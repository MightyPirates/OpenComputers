/**
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2013. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */

package dan200.turtle.api;

/**
 * An enum representing the two different actions that an ITurtleUpgrade of type
 * Tool may be called on to perform by a turtle.
 * @see ITurtleUpgrade
 * @see ITurtleUpgrade#useTool
 */
public enum TurtleVerb
{
	/**
	 * The turtle called turtle.dig(), turtle.digUp() or turtle.digDown()
	 */
	Dig,
	
	/**
	 * The turtle called turtle.attack(), turtle.attackUp() or turtle.attackDown()
	 */
	Attack,
}
