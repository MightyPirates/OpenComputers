package forestry.api.genetics;

import forestry.api.core.EnumHumidity;
import forestry.api.core.EnumTemperature;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * Any housing, hatchery or nest which is a fixed location in the world. 
 */
public interface IHousing {

	/**
	 * @return String containing the login of this housing's owner.
	 */
	String getOwnerName();

	World getWorld();

	int getXCoord();

	int getYCoord();

	int getZCoord();

	int getBiomeId();

	EnumTemperature getTemperature();

	EnumHumidity getHumidity();

	void setErrorState(int state);

	int getErrorOrdinal();

	/**
	 * Adds products to the housing's inventory.
	 * 
	 * @param product
	 *            ItemStack with the product to add.
	 * @param all
	 * @return Boolean indicating success or failure.
	 */
	boolean addProduct(ItemStack product, boolean all);

}
