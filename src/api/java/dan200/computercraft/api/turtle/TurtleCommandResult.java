/**
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2014. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */

package dan200.computercraft.api.turtle;

public final class TurtleCommandResult
{
    private static final TurtleCommandResult s_success = new TurtleCommandResult( true, null, null );
    private static final TurtleCommandResult s_emptyFailure = new TurtleCommandResult( false, null, null );

    public static TurtleCommandResult success()
    {
        return success( null );
    }

    public static TurtleCommandResult success( Object[] results )
    {
        if( results == null || results.length == 0 )
        {
            return s_success;
        }
        else
        {
            return new TurtleCommandResult( true, null, results );
        }
    }

    public static TurtleCommandResult failure()
    {
        return failure( null );
    }

    public static TurtleCommandResult failure( String errorMessage )
    {
        if( errorMessage == null )
        {
            return s_emptyFailure;
        }
        else
        {
            return new TurtleCommandResult( false, errorMessage, null );
        }
    }

    private final boolean m_success;
    private final String m_errorMessage;
    private final Object[] m_results;

    private TurtleCommandResult( boolean success, String errorMessage, Object[] results )
    {
        m_success = success;
        m_errorMessage = errorMessage;
        m_results = results;
    }

    public boolean isSuccess()
    {
        return m_success;
    }

    public String getErrorMessage()
    {
        return m_errorMessage;
    }

    public Object[] getResults()
    {
        return m_results;
    }
}
