/*******************************************************************************
 * Copyright 2011-2014 SirSengir
 * 
 * This work (the API) is licensed under the "MIT" License, see LICENSE.txt for details.
 ******************************************************************************/
package forestry.api.genetics;

/**
 * AlleleManager.alleleRegistry can be cast to this type.
 */
public interface ILegacyHandler {
	void registerLegacyMapping(int id, String uid);

	IAllele getFromLegacyMap(int id);
}
