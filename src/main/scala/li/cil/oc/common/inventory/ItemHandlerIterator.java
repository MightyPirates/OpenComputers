package li.cil.oc.common.inventory;

import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

import java.util.Iterator;

final class ItemHandlerIterator implements Iterator<ItemStack> {
    private final IItemHandler itemHandler;
    private int slot;

    public ItemHandlerIterator(final IItemHandler itemHandler) {
        this.itemHandler = itemHandler;
    }

    // ----------------------------------------------------------------------- //
    // Iterator

    @Override
    public boolean hasNext() {
        return slot < itemHandler.getSlots();
    }

    @Override
    public ItemStack next() {
        return itemHandler.getStackInSlot(slot++);
    }
}
