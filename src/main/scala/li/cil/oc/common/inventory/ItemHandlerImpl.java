package li.cil.oc.common.inventory;

import li.cil.oc.OpenComputers;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.NonNullList;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;

public class ItemHandlerImpl implements IItemHandlerModifiable, INBTSerializable<NBTTagList> {
    // ----------------------------------------------------------------------- //
    // Persisted data.

    private final NonNullList<ItemStack> stacks;

    public ItemHandlerImpl(final int size) {
        stacks = NonNullList.withSize(size, ItemStack.EMPTY);
    }

    // ----------------------------------------------------------------------- //
    // IItemHandler

    @Override
    public int getSlots() {
        return stacks.size();
    }

    @Override
    public ItemStack getStackInSlot(final int slot) {
        return stacks.get(slot);
    }

    @Override
    public ItemStack insertItem(final int slot, final ItemStack stack, final boolean simulate) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        final ItemStack oldStack = stacks.get(slot);
        if (!oldStack.isEmpty() && !ItemHandlerHelper.canItemStacksStack(stack, oldStack)) {
            return stack;
        }

        final int toInsert = Math.min(getStackLimit(slot, stack) - oldStack.getCount(), stack.getCount());
        if (toInsert <= 0) {
            return stack;
        }

        if (!simulate) {
            if (oldStack.isEmpty()) {
                stacks.set(slot, ItemHandlerHelper.copyStackWithSize(stack, toInsert));
                onItemAdded(slot, stacks.get(slot));
            } else {
                oldStack.grow(toInsert);
                onItemChanged(slot, oldStack);
            }
        }

        return ItemHandlerHelper.copyStackWithSize(stack, stack.getCount() - toInsert);
    }

    @Override
    public ItemStack extractItem(final int slot, final int amount, final boolean simulate) {
        if (amount <= 0) {
            return ItemStack.EMPTY;
        }

        final ItemStack oldStack = stacks.get(slot);
        if (oldStack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        final int toExtract = Math.min(amount, oldStack.getCount());
        if (toExtract <= 0) {
            return ItemStack.EMPTY;
        }

        if (oldStack.getCount() == toExtract) {
            if (!simulate) {
                stacks.set(slot, ItemStack.EMPTY);
                onItemRemoved(slot, oldStack);
            }
        } else {
            if (!simulate) {
                oldStack.shrink(toExtract);
                onItemChanged(slot, oldStack);
            }
        }

        return ItemHandlerHelper.copyStackWithSize(oldStack, toExtract);
    }

    @Override
    public int getSlotLimit(final int slot) {
        return 64;
    }

    // ----------------------------------------------------------------------- //
    // IItemHandlerModifiable

    @Override
    public void setStackInSlot(final int slot, final ItemStack stack) {
        final ItemStack oldStack = stacks.get(slot);
        if (ItemStack.areItemStacksEqual(stack, oldStack)) {
            return;
        }
        stacks.set(slot, ItemStack.EMPTY);
        onItemRemoved(slot, oldStack);

        stacks.set(slot, stack.copy());
        onItemAdded(slot, stacks.get(slot));
    }

    // ----------------------------------------------------------------------- //
    // INBTSerializable

    @Override
    public NBTTagList serializeNBT() {
        final NBTTagList nbt = new NBTTagList();
        for (final ItemStack stack : stacks) {
            nbt.appendTag(stack.serializeNBT());
        }
        return nbt;
    }

    @Override
    public void deserializeNBT(final NBTTagList nbt) {
        if (nbt.tagCount() != stacks.size()) {
            OpenComputers.log().warn("stack count mismatch; not loading data.");
            return;
        }

        for (int slot = 0; slot < nbt.tagCount(); slot++) {
            stacks.set(slot, new ItemStack(nbt.getCompoundTagAt(slot)));
        }
    }

    // ----------------------------------------------------------------------- //

    public boolean isItemValidForSlot(final int slot, final ItemStack stack) {
        return true;
    }

    protected void onItemAdded(final int slot, final ItemStack stack) {
    }

    protected void onItemChanged(final int slot, final ItemStack stack) {
    }

    protected void onItemRemoved(final int slot, final ItemStack stack) {
    }

    private int getStackLimit(final int slot, final ItemStack stack) {
        return Math.min(getSlotLimit(slot), stack.getMaxStackSize());
    }
}
