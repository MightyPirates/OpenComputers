/*******************************************************************************
 * Copyright 2011-2014 SirSengir
 * 
 * This work (the API) is licensed under the "MIT" License, see LICENSE.txt for details.
 ******************************************************************************/
package forestry.api.lepidopterology;

import forestry.api.genetics.IAlleleEffect;
import forestry.api.genetics.IEffectData;

public interface IAlleleButterflyEffect extends IAlleleEffect {

	/**
	 * Used by butterflies to trigger effects in the world.
	 * @param butterfly {@link IEntityButterfly}
	 * @param storedData
	 * @return {@link forestry.api.genetics.IEffectData} for the next cycle.
	 */
	IEffectData doEffect(IEntityButterfly butterfly, IEffectData storedData);

}
