package forestry.api.mail;

import net.minecraft.item.ItemStack;

public interface IStamps {

	EnumPostage getPostage(ItemStack itemstack);

}
