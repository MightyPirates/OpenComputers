/*
 * $Id: LuaMemoryAllocationException.java 38 2012-01-04 22:44:15Z andre@naef.com $
 * See LICENSE.txt for license terms.
 */

package com.naef.jnlua;

/**
 * Indicates a Lua memory allocation error.
 * 
 * <p>
 * The exception is thrown if the Lua memory allocator runs out of memory or if
 * a JNI allocation fails.
 * </p>
 */
public class LuaMemoryAllocationException extends LuaException {
	// -- Static
	private static final long serialVersionUID = 1L;

	// -- Construction
	/**
	 * Creates a new instance.
	 * 
	 * @param msg
	 *            the message
	 */
	public LuaMemoryAllocationException(String msg) {
		super(msg);
	}

	/**
	 * Creates a new instance.
	 * 
	 * @param msg
	 *            the message
	 * @param cause
	 *            the cause of this exception
	 */
	public LuaMemoryAllocationException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
