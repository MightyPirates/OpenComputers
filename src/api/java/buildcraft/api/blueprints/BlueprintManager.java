package buildcraft.api.blueprints;

import buildcraft.api.core.BuildCraftAPI;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

@Deprecated
public class BlueprintManager {

	public static BptBlock[] blockBptProps = new BptBlock[Block.blocksList.length];

	public static ItemSignature getItemSignature(Item item) {
		ItemSignature sig = new ItemSignature();

		if (item.itemID >= Block.blocksList.length + BuildCraftAPI.LAST_ORIGINAL_ITEM) {
			sig.itemClassName = item.getClass().getSimpleName();
		}

		sig.itemName = item.getUnlocalizedName(new ItemStack(item));

		return sig;
	}

	public static BlockSignature getBlockSignature(Block block) {
		return BlueprintManager.blockBptProps[0].getSignature(block);
	}

	static {
		// Initialize defaults for block properties.
		for (int i = 0; i < BlueprintManager.blockBptProps.length; ++i) {
			new BptBlock(i);
		}
	}
}
