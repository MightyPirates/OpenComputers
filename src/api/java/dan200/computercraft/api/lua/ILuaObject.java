/**
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2014. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */

package dan200.computercraft.api.lua;

/**
 * An interface for representing custom objects returned by IPeripheral.callMethod() calls.
 * Return objects implementing this interface to expose objects with methods to lua.
 */
public interface ILuaObject
{
	/**
	 * Get the names of the methods that this object implements. This works the same as IPeripheral.getMethodNames(). See that method for detailed documentation.
	 * @see dan200.computercraft.api.peripheral.IPeripheral#getMethodNames()
	 */
    public String[] getMethodNames();

	/**
	 * Called when a user calls one of the methods that this object implements. This works the same as IPeripheral.callMethod(). See that method for detailed documentation.
	 * @see dan200.computercraft.api.peripheral.IPeripheral#callMethod(dan200.computercraft.api.peripheral.IComputerAccess, ILuaContext, int, Object[])
	 */
    public Object[] callMethod( ILuaContext context, int method, Object[] arguments ) throws LuaException, InterruptedException;
}
