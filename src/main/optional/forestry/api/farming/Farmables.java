package forestry.api.farming;

import java.util.Collection;
import java.util.HashMap;

public class Farmables {
	/**
	 * Can be used to add IFarmables to some of the vanilla farm logics.
	 * 
	 * Identifiers: farmArboreal farmWheat farmGourd farmInfernal farmPoales farmSucculentes farmVegetables farmShroom
	 */
	public static HashMap<String, Collection<IFarmable>> farmables = new HashMap<String, Collection<IFarmable>>();

	public static IFarmInterface farmInterface;
}
