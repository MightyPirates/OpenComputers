package forestry.api.arboriculture;

import java.util.EnumSet;

import net.minecraftforge.common.EnumPlantType;
import forestry.api.genetics.IGenome;

public interface ITreeGenome extends IGenome {

	IAlleleTreeSpecies getPrimary();

	IAlleleTreeSpecies getSecondary();

	IFruitProvider getFruitProvider();

	IGrowthProvider getGrowthProvider();

	float getHeight();

	float getFertility();

	/**
	 * @return Determines either a) how many fruit leaves there are or b) the chance for any fruit leave to drop a sapling. Exact usage determined by the
	 *         IFruitProvider
	 */
	float getYield();

	float getSappiness();

	EnumSet<EnumPlantType> getPlantTypes();

	/**
	 * @return Amount of random block ticks required for a sapling to mature into a fully grown tree.
	 */
	int getMaturationTime();

	int getGirth();

	IAlleleLeafEffect getEffect();
}
