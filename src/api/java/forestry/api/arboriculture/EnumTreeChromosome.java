package forestry.api.arboriculture;

import net.minecraftforge.common.EnumPlantType;
import forestry.api.genetics.AlleleManager;
import forestry.api.genetics.IAllele;
import forestry.api.genetics.IAlleleArea;
import forestry.api.genetics.IAlleleFloat;
import forestry.api.genetics.IAlleleInteger;
import forestry.api.genetics.IAllelePlantType;
import forestry.api.genetics.IChromosomeType;
import forestry.api.genetics.IFruitFamily;
import forestry.api.genetics.ISpeciesRoot;

public enum EnumTreeChromosome implements IChromosomeType {

	/**
	 * Determines the following: - WorldGen, including the used wood blocks - {@link IFruitFamily}s supported. Limits which {@link IFruitProvider}
	 * will actually yield fruit with this species. - Native {@link EnumPlantType} for this tree. Combines with the PLANT chromosome.
	 */
	SPECIES(IAlleleTreeSpecies.class),
	/**
	 * {@link IGrowthProvider}, determines conditions required by the tree to grow.
	 */
	GROWTH(IAlleleGrowth.class),
	/**
	 * A float modifying the height of the tree. Taken into account at worldgen.
	 */
	HEIGHT(IAlleleFloat.class),
	/**
	 * Chance for saplings.
	 */
	FERTILITY(IAlleleFloat.class),
	/**
	 * {@link IFruitProvider}, determines if and what fruits are grown on the tree. Limited by the {@link IFruitFamily}s the species supports.
	 */
	FRUITS(IAlleleFruit.class),
	/**
	 * Chance for fruit leaves and/or drops.
	 */
	YIELD(IAlleleFloat.class),
	/**
	 * May add additional tolerances for {@link EnumPlantTypes}.
	 */
	PLANT(IAllelePlantType.class),
	/**
	 * Determines the speed at which fruit will ripen on this tree.
	 */
	SAPPINESS(IAlleleFloat.class),
	/**
	 * Territory for leaf effects. Unused.
	 */
	TERRITORY(IAlleleArea.class),
	/**
	 * Leaf effect. Unused.
	 */
	EFFECT(IAlleleLeafEffect.class),
	/**
	 * Amount of random ticks which need to elapse before a sapling will grow into a tree.
	 */
	MATURATION(IAlleleInteger.class),

	GIRTH(IAlleleInteger.class),
	;
	
	Class<? extends IAllele> clss;
	
	EnumTreeChromosome(Class<? extends IAllele> clss) {
		this.clss = clss;
	}

	@Override
	public Class<? extends IAllele> getAlleleClass() {
		return clss;
	}

	@Override
	public String getName() {
		return this.toString().toLowerCase();
	}

	@Override
	public ISpeciesRoot getSpeciesRoot() {
		return AlleleManager.alleleRegistry.getSpeciesRoot("rootTrees");
	}

}
