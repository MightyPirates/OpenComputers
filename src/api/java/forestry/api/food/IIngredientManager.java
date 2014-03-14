package forestry.api.food;

import net.minecraft.item.ItemStack;

public interface IIngredientManager {

	String getDescription(ItemStack itemstack);

	void addIngredient(ItemStack ingredient, String description);

}
