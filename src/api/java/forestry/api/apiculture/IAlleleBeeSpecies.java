package forestry.api.apiculture;

import java.util.Map;

import net.minecraft.item.ItemStack;
import net.minecraft.util.Icon;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import forestry.api.genetics.IAlleleSpecies;

public interface IAlleleBeeSpecies extends IAlleleSpecies {

	/**
	 * @return the IBeeRoot
	 */
	IBeeRoot getRoot();

	/**
	 * @return true if this species is only active at night.
	 */
	boolean isNocturnal();
	
	/**
	 * @return Map of possible products with the chance for drop each bee cycle. (0 - 100)
	 */
	Map<ItemStack, Integer> getProducts();

	/**
	 * @return Map of possible specialities with the chance for drop each bee cycle. (0 - 100)
	 */
	Map<ItemStack, Integer> getSpecialty();

	/**
	 * Only jubilant bees produce specialities.
	 * @return true if the bee is jubilant, false otherwise.
	 */
	boolean isJubilant(IBeeGenome genome, IBeeHousing housing);

	@SideOnly(Side.CLIENT)
	Icon getIcon(EnumBeeType type, int renderPass);

	/**
	 * @return Path of the texture to use for entity rendering.
	 */
	String getEntityTexture();
}
