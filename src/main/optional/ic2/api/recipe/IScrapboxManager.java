package ic2.api.recipe;

import java.util.Map;

import net.minecraft.item.ItemStack;

public interface IScrapboxManager {
	void addDrop(ItemStack drop, float rawChance);

	ItemStack getDrop(ItemStack input, boolean adjustInput);

	Map<ItemStack, Float> getDrops();
}
