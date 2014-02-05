package forestry.api.world;

import net.minecraft.world.gen.feature.WorldGenerator;

public interface IWorldGenInterface {

	/**
	 * Retrieves generators for trees identified by a given string.
	 * 
	 * Returned generator classes take an {@link ITreeGenData} in the constructor.
	 * 
	 * @param ident
	 *            Unique identifier for tree type. Forestry's convention is 'treeSpecies', i.e. 'treeBaobab', 'treeSequoia'.
	 * @return All generators matching the given ident.
	 */
	Class<? extends WorldGenerator>[] getTreeGenerators(String ident);
}
