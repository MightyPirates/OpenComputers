package li.cil.oc.common.inventory;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.Arrays;

/**
 * Base implementation of an array based inventory.
 */
public class InventoryImpl implements IInventory, INBTSerializable<NBTTagList> {
    private final String name;
    private final ItemStack[] items;

    public InventoryImpl(final String name, final int size) {
        this.name = name;
        Arrays.fill(items = new ItemStack[size], ItemStack.EMPTY);
    }

    // --------------------------------------------------------------------- //
    // INBTSerializable

    @Override
    public NBTTagList serializeNBT() {
        final NBTTagList nbt = new NBTTagList();
        for (final ItemStack stack : items) {
            final NBTTagCompound stackNbt = new NBTTagCompound();
            if (stack != null) {
                stack.writeToNBT(stackNbt);
            }
            nbt.appendTag(stackNbt);
        }
        return nbt;
    }

    @Override
    public void deserializeNBT(NBTTagList nbt) {
        final int count = Math.min(nbt.tagCount(), items.length);
        for (int index = 0; index < count; index++) {
            items[index] = new ItemStack(nbt.getCompoundTagAt(index));
        }

    }

    // --------------------------------------------------------------------- //

    protected void onItemAdded(final int index) {
    }

    protected void onItemRemoved(final int index) {
    }

    // --------------------------------------------------------------------- //
    // IWorldNameable

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean hasCustomName() {
        return false;
    }

    @Override
    public ITextComponent getDisplayName() {
        return hasCustomName() ? new TextComponentString(getName()) : new TextComponentTranslation(getName());
    }

    // --------------------------------------------------------------------- //
    // IInventory

    @Override
    public int getSizeInventory() {
        return items.length;
    }

    public boolean isEmpty() {
        for (final ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public ItemStack getStackInSlot(final int index) {
        return items[index];
    }

    @Override
    public ItemStack decrStackSize(final int index, final int count) {
        if (items[index].getCount() <= count) {
            return removeStackFromSlot(index);
        } else {
            final ItemStack stack = items[index].splitStack(count);
            assert items[index].getCount() > 0;
            markDirty();
            return stack;
        }
    }

    @Override
    public ItemStack removeStackFromSlot(final int index) {
        final ItemStack stack = items[index];
        setInventorySlotContents(index, ItemStack.EMPTY);
        return stack;
    }

    @Override
    public void setInventorySlotContents(final int index, final ItemStack stack) {
        if (items[index] == stack) {
            return;
        }

        if (!items[index].isEmpty()) {
            onItemRemoved(index);
        }

        items[index] = stack;

        if (!items[index].isEmpty()) {
            onItemAdded(index);
        }

        markDirty();
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public void markDirty() {
    }

    @Override
    public boolean isUsableByPlayer(final EntityPlayer player) {
        return true;
    }

    @Override
    public void openInventory(final EntityPlayer player) {
    }

    @Override
    public void closeInventory(final EntityPlayer player) {
    }

    @Override
    public boolean isItemValidForSlot(final int index, final ItemStack stack) {
        return true;
    }

    @Override
    public int getField(final int id) {
        return 0;
    }

    @Override
    public void setField(final int id, final int value) {
    }

    @Override
    public int getFieldCount() {
        return 0;
    }

    @Override
    public void clear() {
    }
}
