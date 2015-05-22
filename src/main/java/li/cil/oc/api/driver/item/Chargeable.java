package li.cil.oc.api.driver.item;

import net.minecraft.item.ItemStack;

/**
 * This interface can be implemented on items that go into a charger.
 * <p/>
 * This provides a generic way of charging items, even such that are not
 * components. Doing it this way enables items to provide another environment,
 * that is unrelated to charging, such as tablets providing their file system,
 * while in the charger.
 */
public interface Chargeable {
    /**
     * Whether the specified item stack can be charged.
     * <p/>
     * This is primarily meant to filter meta item subitems that are not meant
     * to be chargeable.
     *
     * @param stack the stack to check for.
     * @return whether the specified item stack is chargeable.
     */
    boolean canCharge(ItemStack stack);

    /**
     * Called when checking if an item can be charged or should be charged.
     * <p/>
     * To discharge an item, pass a negative value.
     *
     * @param stack    the item to charge.
     * @param amount   the amount to inject into the item.
     * @param simulate whether to only simulate injection.
     * @return the remainder of the energy that could not be injected/extracted.
     */
    double charge(ItemStack stack, double amount, boolean simulate);
}
