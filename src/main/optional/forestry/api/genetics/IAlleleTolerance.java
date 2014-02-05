package forestry.api.genetics;

/**
 * Simple interface to allow adding additional alleles containing float values.
 */
public interface IAlleleTolerance extends IAllele {

	EnumTolerance getValue();

}
