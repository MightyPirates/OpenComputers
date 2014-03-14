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
