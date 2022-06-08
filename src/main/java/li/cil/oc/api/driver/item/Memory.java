package li.cil.oc.api.driver.item;

import li.cil.oc.api.driver.Item;
import net.minecraft.item.ItemStack;

/**
 * Use this interface to implement item drivers extending the memory of a computer.
 * <br>
 * Note that the item must be installed in the actual computer's inventory to
 * work. If it is installed in an external inventory the computer will not
 * recognize the memory.
 */
public interface Memory extends Item {
    /**
     * The amount of RAM this component provides, as a generic scaling factor.
     * <br>
     * This factor has to be interpreted by each individual architecture to fit
     * its own memory needs. The actual values returned here should roughly be
     * equivalent to the item's tier. For example, the built-in memory modules
     * provide defaults of 192 for tier one, 256 for tier 1.5, 384 for tier 2, etc.
     * Mind that those values may be changed in the config file.
     *
     * @param stack the item to get the provided memory for.
     * @return the amount of memory the specified component provides.
     */
    double amount(ItemStack stack);
}
