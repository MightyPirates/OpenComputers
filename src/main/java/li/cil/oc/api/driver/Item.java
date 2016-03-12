package li.cil.oc.api.driver;

import li.cil.oc.api.network.ManagedEnvironment;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Interface for item component drivers.
 * <p/>
 * This driver type is used for components that are items, i.e. that can be
 * inserted into computers and robots. An example for this are internal drives,
 * memory and graphic cards.
 * <p/>
 * When trying to add an item to a computer the list of registered drivers is
 * queried using the drivers' <tt>worksWith</tt> functions. The first driver
 * that replies positively and whose check against the slot type is successful,
 * i.e. for which the <tt>slot</tt> matches the slot it should be inserted into,
 * will be used as the component's driver and the component will be added. If no
 * driver is found the item will be rejected and cannot be installed.
 * <p/>
 * Note that it is possible to write one driver that supports as many different
 * items as you wish. I'd recommend writing one per device (type), though, to
 * keep things modular.
 */
public interface Item {
    /**
     * Used to determine the item types this driver handles.
     * <p/>
     * This is used to determine which driver to use for an item when it should
     * be installed in a computer. Note that the return value should not change
     * over time; if it does, though, an already installed component will not
     * be ejected, since this value is only checked when adding components.
     * <p/>
     * This is a context-agnostic variant used mostly for "house-keeping"
     * stuff, such as querying slot types and tier.
     *
     * @param stack the item to check.
     * @return <tt>true</tt> if the item is supported; <tt>false</tt> otherwise.
     */
    boolean worksWith(ItemStack stack);

    /**
     * Create a new managed environment interfacing the specified item.
     * <p/>
     * This is used to connect the component to the component network when it is
     * added to a computer, for example. The only kind of component that does
     * not need to be connected to the component network is probably memory, and
     * there's a built-in driver for that. You may still opt to not implement
     * this - i.e. it is safe to return <tt>null</tt> here.
     * <p/>
     * Keep in mind that the host's location may change if the owner is
     * a robot. This is important if you cache the location somewhere. For
     * example, the wireless network card checks in a robot movement event
     * handler for position changes to update the index structure used for
     * receiver look-up.
     * <p/>
     * This is expected to return a <em>new instance</em> each time it is
     * called. The created instance's life cycle is managed by the host
     * that caused its creation.
     *
     * @param stack the item stack for which to get the environment.
     * @param host  the host the environment will be managed by.
     * @return the environment for that item.
     */
    ManagedEnvironment createEnvironment(ItemStack stack, li.cil.oc.api.network.EnvironmentHost host);

    /**
     * The slot type of the specified item this driver supports.
     * <p/>
     * This is used to determine into which slot of a computer the components
     * this driver supports may go. This will only be called if a previous call
     * to {@link #worksWith} with the same stack returned true.
     *
     * @param stack the item stack to get the slot type for.
     * @return the slot type of the specified item.
     * @see li.cil.oc.api.driver.item.Slot
     */
    String slot(ItemStack stack);

    /**
     * The tier of the specified item this driver supports.
     * <p/>
     * This is used to determine into which slot of a computer the components
     * this driver supports may go. This will only be called if a previous call
     * to {@link #worksWith} with the same stack returned true.
     * <p/>
     * <em>Important</em>: tiers are zero-indexed.
     *
     * @param stack the item stack to get the tier for.
     * @return the tier of the specified item.
     */
    int tier(ItemStack stack);

    /**
     * Get the tag compound based on the item stack to use for persisting the
     * environment associated with the specified item stack.
     * <p/>
     * This is only used if the item has an environment. This must always be a
     * child tag of the item stack's own tag compound, it will not be saved
     * otherwise. Use this in the unlikely case that the default name collides
     * with something. The built-in components use a child tag-compound with
     * the name <tt>oc:data</tt>, which will also be used if this returns
     * <tt>null</tt>.
     * <p/>
     * This tag will be passed to the environment's <tt>save</tt> and
     * <tt>load</tt> methods when appropriate (world save / load and when
     * removed from their hosting inventory).
     *
     * @param stack the item to get the child tag from.
     * @return the tag to use for saving and loading, or <tt>null</tt> to use
     * the default tag <tt>oc:data</tt>.
     */
    NBTTagCompound dataTag(ItemStack stack);
}