/*******************************************************************************
 * Copyright 2011-2014 SirSengir
 * 
 * This work (the API) is licensed under the "MIT" License, see LICENSE.txt for details.
 ******************************************************************************/
package forestry.api.genetics;

import forestry.api.core.INBTTagable;

/**
 * Container to hold some temporary data for bee, tree and butterfly effects.
 * 
 * @author SirSengir
 */
public interface IEffectData extends INBTTagable {
	void setInteger(int index, int val);

	void setFloat(int index, float val);

	void setBoolean(int index, boolean val);

	int getInteger(int index);

	float getFloat(int index);

	boolean getBoolean(int index);
}
