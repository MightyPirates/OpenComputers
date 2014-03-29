/**
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2014. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */

package dan200.computercraft.api.turtle;

/**
 * An interface for objects executing custom turtle commands, used with ITurtleAccess.issueCommand
 * @see ITurtleAccess#issueCommand(ITurtleCommand)
 */
public interface ITurtleCommand
{
	/**
	 * Will be called by the turtle on the main thread when it is time to execute the custom command.
	 * The handler should either perform the work of the command, and return success, or return
	 * failure with an error message to indicate the command cannot be executed at this time.
	 * @param turtle access to the turtle for whom the command was issued
	 * @return TurtleCommandResult.success() or TurtleCommandResult.failure( errorMessage )
 	 * @see ITurtleAccess#issueCommand(ITurtleCommand)
     * @see dan200.computercraft.api.turtle.TurtleCommandResult
	 */
	public TurtleCommandResult execute( ITurtleAccess turtle );
}
