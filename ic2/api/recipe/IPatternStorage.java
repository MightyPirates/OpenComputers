package ic2.api.recipe;

import net.minecraft.item.ItemStack;

public interface IPatternStorage {

	boolean transferPattern(ItemStack itemstack, int amountUU , int amountEU);

	int[] getPatternvalus(ItemStack itemstack);

	short getPatternCount();

	ItemStack getPatternItemstack(int index);

}
