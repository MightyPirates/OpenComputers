package forestry.api.arboriculture;

public interface ITreeModifier {

	/**
	 * 
	 * @param genome
	 * @return Float used to modify the height.
	 */
	float getHeightModifier(ITreeGenome genome, float currentModifier);

	/**
	 * 
	 * @param genome
	 * @return Float used to modify the yield.
	 */
	float getYieldModifier(ITreeGenome genome, float currentModifier);

	/**
	 * 
	 * @param genome
	 * @return Float used to modify the sappiness.
	 */
	float getSappinessModifier(ITreeGenome genome, float currentModifier);

	/**
	 * 
	 * @param genome
	 * @return Float used to modify the maturation.
	 */
	float getMaturationModifier(ITreeGenome genome, float currentModifier);

	/**
	 * @param genome0
	 * @param genome1
	 * @return Float used to modify the base mutation chance.
	 */
	float getMutationModifier(ITreeGenome genome0, ITreeGenome genome1, float currentModifier);

}
