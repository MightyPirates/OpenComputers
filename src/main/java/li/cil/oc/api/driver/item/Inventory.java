package li.cil.oc.api.driver.item;

import li.cil.oc.api.driver.Item;
import net.minecraft.item.ItemStack;

/**
 * This interface marks drivers for robot upgrades that provide inventory
 * space to the robot. Note that this is only queried upon robot assembly,
 * after that the inventory is fixed! This in particular means that there
 * can be no hot-swappable inventories - at least none that are represented
 * in the GUI.
 */
public interface Inventory extends Item {
    /**
     * The additional amount of inventory space the specified item provides.
     *
     * @param stack the item to get the provided inventory space for.
     * @return the provided inventory space.
     */
    int inventoryCapacity(ItemStack stack);
}
