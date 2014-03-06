/*
 * $Id: NamedJavaFunction.java 38 2012-01-04 22:44:15Z andre@naef.com $
 * See LICENSE.txt for license terms.
 */

package com.naef.jnlua;

/**
 * Provides a named Java function.
 */
public interface NamedJavaFunction extends JavaFunction {
	/**
	 * Returns the name of this Java function.
	 * 
	 * @return the Java function name
	 */
	public String getName();
}
