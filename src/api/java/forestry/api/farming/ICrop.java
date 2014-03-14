package forestry.api.farming;

import java.util.Collection;

import net.minecraft.item.ItemStack;

public interface ICrop {

	/**
	 * Harvests this crop. Performs the necessary manipulations to set the crop into a "harvested" state.
	 * 
	 * @return Products harvested.
	 */
	Collection<ItemStack> harvest();

}
