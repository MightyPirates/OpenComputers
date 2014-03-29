/**
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2014. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */

package dan200.computercraft.api.turtle;

public final class TurtleCommandResult
{
    private static final TurtleCommandResult s_success = new TurtleCommandResult( true, null );
    private static final TurtleCommandResult s_emptyFailure = new TurtleCommandResult( false, null );

    public static TurtleCommandResult success()
    {
        return s_success;
    }

    public static TurtleCommandResult failure()
    {
        return failure( null );
    }

    public static TurtleCommandResult failure( String errorMessage )
    {
        if( errorMessage != null )
        {
            return new TurtleCommandResult( false, errorMessage );
        }
        else
        {
            return s_emptyFailure;
        }
    }

    private final boolean m_success;
    private final String m_errorMessage;

    private TurtleCommandResult( boolean success, String errorMessage )
    {
        m_success = success;
        m_errorMessage = errorMessage;
    }

    public boolean isSuccess()
    {
        return m_success;
    }

    public String getErrorMessage()
    {
        return m_errorMessage;
    }
}
