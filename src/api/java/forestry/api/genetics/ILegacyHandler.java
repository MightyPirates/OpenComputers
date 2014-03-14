package forestry.api.genetics;

/**
 * AlleleManager.alleleRegistry can be cast to this type.
 */
public interface ILegacyHandler {
	void registerLegacyMapping(int id, String uid);

	IAllele getFromLegacyMap(int id);
}
