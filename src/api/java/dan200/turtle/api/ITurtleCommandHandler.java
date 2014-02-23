/**
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2013. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */

package dan200.turtle.api;

/**
 * An interface for objects executing custom turtle commands, used with ITurtleAccess.issueCommand
 * @see ITurtleAccess#issueCommand( ITurtleCommandHandler )
 */
public interface ITurtleCommandHandler
{
	/**
	 * Will be called by the turtle on the main thread when it is time to execute the custom command.
	 * The handler should either perform the work of the command, and return true for success, or return
	 * false to indicate failure if the command cannot be executed at this time.
	 * @param turtle access to the turtle for whom the command was issued
	 * @return true for success, false for failure. If true is returned, the turtle will wait 0.4 seconds
	 * before executing the next command in its queue, as it does for the standard turtle commands.
 	 * @see ITurtleAccess#issueCommand( ITurtleCommandHandler )
	 */
	public boolean handleCommand( ITurtleAccess turtle );
}
