package li.cil.oc.common.inventory;

import net.minecraft.item.ItemStack;

public class ItemHandlerHosted extends ItemHandlerImpl {
    public interface ItemHandlerHost {
        default void onItemAdded(final int slot, final ItemStack stack) {
        }

        default void onItemChanged(final int slot, final ItemStack stack) {
        }

        default void onItemRemoved(final int slot, final ItemStack stack) {
        }
    }

    // ----------------------------------------------------------------------- //
    // Computed data.

    private final ItemHandlerHost host;

    // ----------------------------------------------------------------------- //

    public ItemHandlerHosted(final ItemHandlerHost host, final int size) {
        super(size);
        this.host = host;
    }

    protected ItemHandlerHost getHost() {
        return host;
    }

    // ----------------------------------------------------------------------- //
    // ItemHandlerImpl

    @Override
    protected void onItemAdded(final int slot, final ItemStack stack) {
        host.onItemAdded(slot, stack);
    }

    @Override
    protected void onItemChanged(final int slot, final ItemStack stack) {
        host.onItemChanged(slot, stack);
    }

    @Override
    protected void onItemRemoved(final int slot, final ItemStack stack) {
        host.onItemRemoved(slot, stack);
    }
}
