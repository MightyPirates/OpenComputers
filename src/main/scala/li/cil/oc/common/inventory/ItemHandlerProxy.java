package li.cil.oc.common.inventory;

import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;

public interface ItemHandlerProxy extends IItemHandler {
    IItemHandler getItemHandler();

    @Override
    default int getSlots() {
        return getItemHandler().getSlots();
    }

    @Nonnull
    @Override
    default ItemStack getStackInSlot(int slot) {
        return getItemHandler().getStackInSlot(slot);
    }

    @Nonnull
    @Override
    default ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
        return getItemHandler().insertItem(slot, stack, simulate);
    }

    @Nonnull
    @Override
    default ItemStack extractItem(int slot, int amount, boolean simulate) {
        return getItemHandler().extractItem(slot, amount, simulate);
    }

    @Override
    default int getSlotLimit(int slot) {
        return getItemHandler().getSlotLimit(slot);
    }
}
