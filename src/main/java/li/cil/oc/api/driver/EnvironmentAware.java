package li.cil.oc.api.driver;

import li.cil.oc.api.network.Environment;
import net.minecraft.item.ItemStack;

/**
 * This interface can be added to either item or block drivers.
 * <p/>
 * It is used to statically query the type of environment that would be created
 * for the block or item represented by an item stack. This is used to provide
 * automatically generated ingame help in the NEI usage screen, for example.
 * <p/>
 * For item drivers this will usually be pretty simple to implement, assuming
 * the driver only ever generates one type of environment - just return the
 * class of it and you're done.
 * <p/>
 * For block drivers there is a bit more work involved, since you have to check
 * if the item is the item block that corresponds to the supported block type.
 * This should usually not be an issue either, though.
 */
public interface EnvironmentAware {
    /**
     * Get the type of environment that would be created for the specified
     * block or item.
     * <p/>
     * Note that for block drivers this is called for any type of item stack.
     * <p/>
     * For item drivers this is only called if {@link Item#worksWith(net.minecraft.item.ItemStack)}
     * returns <tt>true</tt>.
     *
     * @param stack the item stack representing a block or item to get the
     *              related environment type for.
     * @return the type of environment this driver would produce, or
     * <tt>null</tt> if the block or item is not supported.
     */
    Class<? extends Environment> providedEnvironment(ItemStack stack);
}
