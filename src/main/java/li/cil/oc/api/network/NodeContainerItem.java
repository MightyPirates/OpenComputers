package li.cil.oc.api.network;

import net.minecraft.item.ItemStack;

/**
 * This kind of environment is managed by component inventories, such as a
 * computer or floppy drive, for environments provided by a {@link li.cil.oc.api.driver.DriverItem}.
 */
public interface NodeContainerItem extends NodeContainer {
    void onInstalled(final ItemStack stack);

    void onUninstalled(final ItemStack stack);

    void onDispose();
}
