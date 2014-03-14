package forestry.api.genetics;

import java.util.HashMap;

import net.minecraft.item.ItemStack;

/**
 * Holds a static reference to the {@link IAlleleRegistry}. 
 */
public class AlleleManager {
	/**
	 * Main access point for all things related to genetics. See {@link IAlleleRegistry} for details.
	 */
	public static IAlleleRegistry alleleRegistry;
	/**
	 * Translates plain leaf blocks into genetic data. Used by bees and butterflies to convert and pollinate foreign leaf blocks.
	 */
	public static HashMap<ItemStack, IIndividual> ersatzSpecimen = new HashMap<ItemStack, IIndividual>();
	/**
	 * Translates plain saplings into genetic data. Used by the treealyzer and the farm to convert foreign saplings.
	 */
	public static HashMap<ItemStack, IIndividual> ersatzSaplings = new HashMap<ItemStack, IIndividual>();

}
