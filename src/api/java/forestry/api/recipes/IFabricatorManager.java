package forestry.api.recipes;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

public interface IFabricatorManager extends ICraftingProvider {

	void addRecipe(ItemStack plan, FluidStack molten, ItemStack result, Object[] pattern);

	void addSmelting(ItemStack resource, FluidStack molten, int meltingPoint);

}
