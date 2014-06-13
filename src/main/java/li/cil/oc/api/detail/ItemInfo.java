package li.cil.oc.api.detail;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public interface ItemInfo {
    /**
     * The name of this item descriptor.
     * <p/>
     * This is the name that yields this instance when passed to
     * {@link li.cil.oc.api.Items#get(String)}. Useful for reverse-lookup when
     * retrieving the descriptor via an item stack.
     *
     * @return the name of this item descriptor.
     */
    String name();

    /**
     * Returns the block type of the represented item. In case the item is not
     * a block this will return <tt>null</tt>.
     * <p/>
     * Note that OpenComputers represents most of its items using just a few
     * actual <tt>Block</tt> instances, so descriptors for different blocks may
     * return the same object here.
     *
     * @return the block type of the represented block.
     */
    Block block();

    /**
     * Returns the item type of the represented item. In case the item is a
     * blocks this will return <tt>null</tt>.
     * <p/>
     * Note that OpenComputers represents most of its items using just a few
     * actual <tt>Item</tt> instances, so descriptors for different items may
     * return the same object here.
     *
     * @return the item type of the represented item.
     */
    Item item();

    /**
     * Creates a new item stack of the item represended by this descriptor.
     *
     * @param size the size of the item stack to create.
     * @return the created item stack.
     */
    ItemStack createItemStack(int size);
}
