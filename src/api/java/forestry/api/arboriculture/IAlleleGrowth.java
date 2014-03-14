package forestry.api.arboriculture;

import forestry.api.genetics.IAllele;

/**
 * Simple allele encapsulating an {@link IGrowthProvider}.
 */
public interface IAlleleGrowth extends IAllele {

	IGrowthProvider getProvider();

}
