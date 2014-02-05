package forestry.api.lepidopterology;

import forestry.api.genetics.IAllele;
import forestry.api.genetics.IGenome;
import forestry.api.genetics.IMutation;

public interface IButterflyMutation extends IMutation {
	float getChance(IButterflyNursery housing, IAllele allele0, IAllele allele1, IGenome genome0, IGenome genome1);
}
