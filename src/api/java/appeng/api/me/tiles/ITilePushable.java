package appeng.api.me.tiles;

import net.minecraft.item.ItemStack;

/**
 * An IPushable should return what dosn't fit, so that the crafting request cannot complete,
 *  stalling the action and saving your resources.
 */
public interface ITilePushable
{
	/**
	 * is this pushable busy?
	 * @return
	 */
	boolean isBusy();
	
	/**
	 * Attempt to send an item.
	 * @param out
	 * @return
	 */
	ItemStack pushItem( ItemStack out );
	
	/**
	 * Test if a push is possible.
	 * @param out
	 * @return
	 */
	boolean canPushItem(ItemStack out);
}
