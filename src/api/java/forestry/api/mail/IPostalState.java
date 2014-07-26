/*******************************************************************************
 * Copyright 2011-2014 SirSengir
 * 
 * This work (the API) is licensed under the "MIT" License, see LICENSE.txt for details.
 ******************************************************************************/
package forestry.api.mail;

public interface IPostalState {
	boolean isOk();

	String getIdentifier();
	
	int ordinal();
}
