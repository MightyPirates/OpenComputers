package forestry.api.food;

import net.minecraft.item.ItemStack;

public interface IInfuserManager {

	void addMixture(int meta, ItemStack ingredient, IBeverageEffect effect);

	void addMixture(int meta, ItemStack[] ingredients, IBeverageEffect effect);

	ItemStack getSeasoned(ItemStack base, ItemStack[] ingredients);

	boolean hasMixtures(ItemStack[] ingredients);

	ItemStack[] getRequired(ItemStack[] ingredients);

}
