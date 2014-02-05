package forestry.api.genetics;

import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import forestry.api.core.EnumHumidity;
import forestry.api.core.EnumTemperature;
import forestry.api.core.IIconProvider;

/**
 * Basic species allele. 
 */
public interface IAlleleSpecies extends IAllele {
	
	/**
	 * @return the {@link ISpeciesRoot} associated with this species.
	 */
	ISpeciesRoot getRoot();

	/**
	 * @return Localized short description of this species. (May be null.)
	 */
	String getDescription();

	/**
	 * Binomial name of the species sans genus ("Apis"). Returning "humboldti" will have the bee species flavour name be "Apis humboldti". Feel free to use fun
	 * names or return null.
	 * 
	 * @return flavour text (may be null)
	 */
	String getBinomial();

	/**
	 * Authority for the binomial name, e.g. "Sengir" on species of base Forestry.
	 * 
	 * @return flavour text (may be null)
	 */
	String getAuthority();

	/**
	 * @return Branch this species is associated with.
	 */
	IClassification getBranch();

	/* RESEARCH */
	/**
	 * Complexity determines the difficulty researching a species. The values of primary and secondary are
	 * added together (and rounded) to determine the amount of pairs needed for successful research.
	 * @return Values between 3 - 11 are useful.
	 */
	int getComplexity();
	
	/**
	 * @param itemstack
	 * @return A float signifying the chance for the passed itemstack to yield a research success.
	 */
	float getResearchSuitability(ItemStack itemstack);
	
	/**
	 * @param world
	 * @param researcher
	 * @param individual
	 * @param bountyLevel
	 * @return Array of itemstacks representing the bounty for this research success.
	 */
	ItemStack[] getResearchBounty(World world, String researcher, IIndividual individual, int bountyLevel);
	
	/* CLIMATE */
	/**
	 * @return Preferred temperature
	 */
	EnumTemperature getTemperature();

	/**
	 * @return Preferred humidity
	 */
	EnumHumidity getHumidity();

	/* MISC */
	/**
	 * @return true if the species icon should have a glowing effect.
	 */
	boolean hasEffect();

	/**
	 * @return true if the species should not be displayed in NEI or creative inventory.
	 */
	boolean isSecret();

	/**
	 * @return true to have the species count against the species total.
	 */
	boolean isCounted();

	/* APPEARANCE */
	/**
	 * @param renderPass Render pass to get the colour for.
	 * @return Colour to use for the render pass.
	 */
	int getIconColour(int renderPass);

	@SideOnly(Side.CLIENT)
	IIconProvider getIconProvider();

}
