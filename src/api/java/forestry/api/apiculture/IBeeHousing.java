package forestry.api.apiculture;

import net.minecraft.item.ItemStack;
import forestry.api.genetics.IHousing;

public interface IBeeHousing extends IBeeModifier, IBeeListener, IHousing {

	ItemStack getQueen();

	ItemStack getDrone();

	void setQueen(ItemStack itemstack);

	void setDrone(ItemStack itemstack);

	/**
	 * @return true if princesses and drones can (currently) mate in this housing to generate queens.
	 */
	boolean canBreed();

}
