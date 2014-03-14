package forestry.api.arboriculture;

import forestry.api.genetics.IAllele;

/**
 * Simple allele encapsulating an {@link IFruitProvider}.
 */
public interface IAlleleFruit extends IAllele {

	IFruitProvider getProvider();

}
