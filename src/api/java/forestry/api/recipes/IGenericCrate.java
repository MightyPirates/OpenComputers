package forestry.api.recipes;

import net.minecraft.item.ItemStack;

public interface IGenericCrate {

	void setContained(ItemStack crate, ItemStack contained);

	ItemStack getContained(ItemStack crate);

}
