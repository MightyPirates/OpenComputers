/*******************************************************************************
 * Copyright 2011-2014 SirSengir
 * 
 * This work (the API) is licensed under the "MIT" License, see LICENSE.txt for details.
 ******************************************************************************/
package forestry.api.genetics;

/**
 * Basic effect allele. 
 */
public interface IAlleleEffect extends IAllele {
	/**
	 * @return true if this effect can combine with the effect on other allele (i.e. run before or after). combination can only occur if both effects are
	 *         combinable.
	 */
	boolean isCombinable();

	/**
	 * Returns the passed data storage if it is valid for this effect or a new one if the passed storage object was invalid for this effect.
	 * 
	 * @param storedData
	 * @return {@link IEffectData} for the next cycle.
	 */
	IEffectData validateStorage(IEffectData storedData);

}
