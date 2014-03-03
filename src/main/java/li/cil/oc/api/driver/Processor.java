package li.cil.oc.api.driver;

import net.minecraft.item.ItemStack;

/**
 * Use this interface to implement item drivers extending the number of
 * components a server can control.
 * <p/>
 * Note that the item must be installed in the actual server's inventory to
 * work. If it is installed in an external inventory the server will not
 * recognize the memory.
 */
public interface Processor extends Item {
    /**
     * The additional number of components supported if this processor is
     * installed in the server.
     *
     * @param stack the processor to get the number of supported components for.
     * @return the number of additionally supported components.
     */
    int supportedComponents(ItemStack stack);
}
