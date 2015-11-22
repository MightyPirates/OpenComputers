package li.cil.oc.api.driver;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

/**
 * Inventory providers are used to access contents of item inventories.
 * <p/>
 * In OpenComputers an example for this would be servers, in other mods
 * this can be backpacks and the like. Inventory providers are used to
 * allow agents (robots, drones) to interact with such inventories using
 * the inventory controller upgrade, for example.
 * <p/>
 * Implementations returned by {@link #getInventory(ItemStack)} should
 * save changes back to the item stack when {@link IInventory#markDirty()}
 * is called. Return <tt>null</tt> if the specified stack is not supported.
 */
public interface InventoryProvider {
    /**
     * Get an inventory implementation that allows interfacing with the
     * item inventory represented by the specified item stack.
     *
     * @param stack the item stack to get the inventory for.
     * @return the inventory representing the contents, or <tt>null</tt>.
     */
    IInventory getInventory(ItemStack stack);
}
