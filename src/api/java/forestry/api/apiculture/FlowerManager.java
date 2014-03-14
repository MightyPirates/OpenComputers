package forestry.api.apiculture;

import java.util.ArrayList;

import forestry.api.genetics.IFlowerProvider;

import net.minecraft.item.ItemStack;

public class FlowerManager {
	/**
	 * ItemStacks representing simple flower blocks. Meta-sensitive, processed by the basic {@link IFlowerProvider}.
	 */
	public static ArrayList<ItemStack> plainFlowers = new ArrayList<ItemStack>();
}
