package li.cil.oc.api.driver;

import net.minecraft.item.ItemStack;

/**
 * May be implemented by drivers for robot upgrades that act as hotswap bays,
 * i.e. which can be installed into the 'dynamic' slots, and provide on-the-fly
 * changeable upgrade slots (i.e. which can be changed in the robot GUI,
 * without disassembling and re-assembling the robot).
 * <p/>
 * These drivers will not be queried for environments. The reported tier is the
 * maximum tier supported in the dynamic slot they provide.
 */
public interface UpgradeContainer extends Item {
    /**
     * The type of slot provided as the dynamic slot. This will usually be
     * for other upgrades, but may be for any type of item component.
     * <p/>
     * While the driver's own type implicitly has to be 'Upgrade' and could
     * therefore be used instead, this makes the intention more clear.
     *
     * @param stack the item stack to get the provided slot type for.
     * @return the slot type provided by that dynamic slot upgrade.
     */
    Slot providedSlot(ItemStack stack);

    /**
     * The maximum item tier of the items that can be placed into the slot
     * provided by the specified container.
     * <p/>
     * This will usually be equal to the container's tier.
     *
     * @param stack the item stack to the the supported tier for.
     * @return the maximum tier supported by that dynamic slot upgrade.
     */
    int providedTier(ItemStack stack);
}
