/*******************************************************************************
 * Copyright 2011-2014 SirSengir
 * 
 * This work (the API) is licensed under the "MIT" License, see LICENSE.txt for details.
 ******************************************************************************/
package forestry.api.genetics;

import forestry.api.core.INBTTagable;

/**
 * Implementations other than Forestry's default one are not supported!
 * 
 * @author SirSengir
 */
public interface IChromosome extends INBTTagable {

	IAllele getPrimaryAllele();

	IAllele getSecondaryAllele();

	IAllele getInactiveAllele();

	IAllele getActiveAllele();

}
