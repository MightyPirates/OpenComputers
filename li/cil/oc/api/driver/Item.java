package li.cil.oc.api.driver;

import li.cil.oc.api.network.environment.ManagedEnvironment;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Interface for item component drivers.
 * <p/>
 * This driver type is used for components that are items, i.e. that can be
 * inserted into computers. An example for this are internal drives, memory and
 * power supply units.
 * <p/>
 * When trying to add an item to a computer the list of registered drivers is
 * queried using the drivers' `worksWith` functions. The first driver that
 * replies positively and whose check against the slot type is successful, i.e.
 * for which the `slot` matches the slot it should be inserted into, will be
 * used as the component's driver and the component will be added. If no driver
 * is found the item will be rejected and cannot be installed.
 * <p/>
 * Note that it is possible to write one driver that supports as many different
 * items as you wish. I'd recommend writing one per device (type), though, to
 * keep things modular.
 */
public interface Item {
    /**
     * Used to determine the item types this driver handles.
     * <p/>
     * This is used to determine which driver to use for an item when installed in
     * a computer. Note that the return value should not change over time; if it
     * does, though, an already installed component will not be ejected, since
     * this value is only checked when adding components.
     *
     * @param item the item to check.
     * @return true if the item is supported; false otherwise.
     */
    boolean worksWith(ItemStack item);

    /**
     * Gets a reference to the network node interfacing the specified item.
     * <p/>
     * This is used to connect the component to the component network when it is
     * added to a computer, for example. Components that are not part of the
     * component network probably don't make much sense (can't think of any uses
     * at this time), but you may still opt to not implement this.
     * <p/>
     * This is expected to return a *new instance* each time it is called.
     *
     * @param item the item instance for which to get the node.
     * @return the network node for that item.
     */
    ManagedEnvironment createEnvironment(ItemStack item);

    /**
     * The slot type of the specified item this driver supports.
     * <p/>
     * This is used to determine into which slot of a computer the components this
     * driver supports may go. This will only be called if a previous call to
     * `worksWith` with the same item type returned true.
     *
     * @param item the item to get the slot type for.
     * @return the component type of the specified item.
     */
    Slot slot(ItemStack item);

    /**
     * Get the tag compound based on the item stack to use for persisting the
     * environment and node associated with the specified item stack.
     * <p/>
     * This is only used if the item has an environment. This must always be a
     * child tag of the items own tag compound, it will not be saved otherwise.
     * Use this in the unlikely case that the default name collides with
     * something. The built-in components use a child tag-compound with the name
     * "oc.node".
     *
     * @param item the item to get the child tag from.
     * @return the tag to use for saving and loading.
     */
    NBTTagCompound nbt(ItemStack item);
}