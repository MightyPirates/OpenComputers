package cofh.api.world;

import net.minecraft.item.ItemStack;
import net.minecraft.util.WeightedRandomItem;

/**
 * This class essentially allows for ores to be generated in clusters, with Features randomly choosing one or more blocks from a weighted list.
 * 
 * @author King Lemming
 * 
 */
public final class WeightedRandomBlock extends WeightedRandomItem {

	public final int blockId;
	public final int metadata;

	public WeightedRandomBlock(ItemStack ore) {

		super(100);
		this.blockId = ore.itemID;
		this.metadata = ore.getItemDamage();
	}

	public WeightedRandomBlock(ItemStack ore, int weight) {

		super(weight);
		this.blockId = ore.itemID;
		this.metadata = ore.getItemDamage();
	}

}
