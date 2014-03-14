package forestry.api.apiculture;

import java.util.ArrayList;

import net.minecraft.item.ItemStack;
import forestry.api.genetics.IEffectData;
import forestry.api.genetics.IIndividual;
import forestry.api.genetics.IIndividualLiving;

/**
 * Other implementations than Forestry's default one are not supported.
 * 
 * @author SirSengir
 */
public interface IBee extends IIndividualLiving {

	/**
	 * @return Bee's genetic information.
	 */
	IBeeGenome getGenome();

	/**
	 * @return Genetic information of the bee's mate, null if unmated.
	 */
	IBeeGenome getMate();

	/**
	 * @return true if the individual is originally of natural origin.
	 */
	boolean isNatural();

	/**
	 * @return generation this individual is removed from the original individual.
	 */
	int getGeneration();

	/**
	 * Set the natural flag on this bee.
	 * @param flag
	 */
	void setIsNatural(boolean flag);

	/**
	 * @return true if the bee is mated with another whose isNatural() doesn't match.
	 */
	boolean isIrregularMating();

	IEffectData[] doEffect(IEffectData[] storedData, IBeeHousing housing);

	IEffectData[] doFX(IEffectData[] storedData, IBeeHousing housing);

	/**
	 * @return true if the bee may spawn offspring
	 */
	boolean canSpawn();

	/**
	 * Determines whether the queen can work.
	 * 
	 * @param housing the {@link IBeeHousing} the bee currently resides in.
	 * @return Ordinal of the error code encountered. 0 - EnumErrorCode.OK
	 */
	int isWorking(IBeeHousing housing);

	boolean hasFlower(IBeeHousing housing);

	ArrayList<Integer> getSuitableBiomeIds();

	ItemStack[] getProduceList();

	ItemStack[] getSpecialtyList();

	ItemStack[] produceStacks(IBeeHousing housing);

	IBee spawnPrincess(IBeeHousing housing);

	IBee[] spawnDrones(IBeeHousing housing);

	void plantFlowerRandom(IBeeHousing housing);

	IIndividual retrievePollen(IBeeHousing housing);

	boolean pollinateRandom(IBeeHousing housing, IIndividual pollen);

}
