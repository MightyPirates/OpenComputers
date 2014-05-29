package li.cil.oc.api;

import li.cil.oc.api.detail.ItemAPI;
import li.cil.oc.api.detail.ItemInfo;
import net.minecraft.item.ItemStack;

/**
 * Access to item definitions for all blocks and items provided by
 * OpenComputers.
 */
public final class Items {
    /**
     * Get a descriptor object for the block or item with the specified name.
     * <p/>
     * The names are the same as the ones used in the recipe files. An info
     * object can be used to retrieve both the block and item instance of the
     * item, if available. It can also be used to create a new item stack of
     * the item.
     *
     * @param name the name of the item to get the descriptor for.
     * @return the descriptor for the item with the specified name, or
     * <tt>null</tt> if there is no such item.
     */
    public static ItemInfo get(String name) {
        if (instance != null)
            return instance.get(name);
        return null;
    }

    /**
     * Get a descriptor object for the block or item represented by the
     * specified item stack.
     *
     * @param stack the stack to get the descriptor for.
     * @return the descriptor for the specified item stack, or <tt>null</tt>
     * if the stack is not a valid OpenComputers item or block.
     */
    public static ItemInfo get(ItemStack stack) {
        if (instance != null)
            return instance.get(stack);
        return null;
    }

    // ----------------------------------------------------------------------- //

    private Items() {
    }

    public static ItemAPI instance = null;
}
