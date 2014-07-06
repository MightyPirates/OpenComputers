/**
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2014. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */

package dan200.computercraft.api.lua;

/**
 * An exception representing an error in Lua, like that raised by the error() function
 */
public class LuaException extends Exception
{
    private final int m_level;

    public LuaException( String message )
    {
        this( message, 1 );
    }

    public LuaException( String message, int level )
    {
        super( message );
        m_level = level;
    }

    public int getLevel()
    {
        return m_level;
    }
}
