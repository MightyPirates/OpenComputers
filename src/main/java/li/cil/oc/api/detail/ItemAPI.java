package li.cil.oc.api.detail;

import net.minecraft.item.ItemStack;

public interface ItemAPI {
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
    ItemInfo get(String name);

    /**
     * Get a descriptor object for the block or item represented by the
     * specified item stack.
     *
     * @param stack the stack to get the descriptor for.
     * @return the descriptor for the specified item stack, or <tt>null</tt>
     * if the stack is not a valid OpenComputers item or block.
     */
    ItemInfo get(ItemStack stack);
}
