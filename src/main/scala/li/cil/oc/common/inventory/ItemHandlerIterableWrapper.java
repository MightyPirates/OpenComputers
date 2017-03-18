package li.cil.oc.common.inventory;

import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

import java.util.Iterator;

public final class ItemHandlerIterableWrapper implements ItemHandlerProxy, Iterable<ItemStack> {
    private final IItemHandler itemHandler;

    // ----------------------------------------------------------------------- //

    public ItemHandlerIterableWrapper(final IItemHandler itemHandler) {
        this.itemHandler = itemHandler;
    }

    // ----------------------------------------------------------------------- //
    // Iterable

    @Override
    public IItemHandler getItemHandler() {
        return itemHandler;
    }

    @Override
    public Iterator<ItemStack> iterator() {
        return new ItemHandlerIterator(itemHandler);
    }
}
