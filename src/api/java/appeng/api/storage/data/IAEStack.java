package appeng.api.storage.data;

import io.netty.buffer.ByteBuf;

import java.io.IOException;

import net.minecraft.nbt.NBTTagCompound;
import appeng.api.config.FuzzyMode;
import appeng.api.storage.StorageChannel;

public interface IAEStack<StackType extends IAEStack>
{

	/**
	 * add two stacks together
	 * 
	 * @param is
	 */
	void add(StackType is);

	/**
	 * number of items in the stack.
	 * 
	 * @return basically ItemStack.stackSize
	 */
	long getStackSize();

	/**
	 * changes the number of items in the stack.
	 * 
	 * @param basically
	 *            , ItemStack.stackSize = N
	 */
	StackType setStackSize(long stackSize);

	/**
	 * Same as getStackSize, but for requestable items. ( LP )
	 * 
	 * @return basically itemStack.stackSize but for requestable items.
	 */
	long getCountRequestable();

	/**
	 * Same as setStackSize, but for requestable items. ( LP )
	 * 
	 * @return basically itemStack.stackSize = N but for setStackSize items.
	 */
	StackType setCountRequestable(long countRequestable);

	/**
	 * true, if the item can be crafted.
	 * 
	 * @return true, if it can be crafted.
	 */
	boolean isCraftable();

	/**
	 * change weather the item can be crafted.
	 * 
	 * @param isCraftable
	 */
	StackType setCraftable(boolean isCraftable);

	/**
	 * clears, requsetable, craftable, and stack sizes.
	 */
	StackType reset();

	/**
	 * returns true, if the item can be crafted, requested, or extracted.
	 * 
	 * @return isThisRecordMeaningful
	 */
	boolean isMeaninful();

	/**
	 * Adds more to the stack size...
	 * 
	 * @param i
	 */
	void incStackSize(long i);

	/**
	 * removes some from the stack size.
	 */
	void decStackSize(long i);

	/**
	 * adds items to the requestable
	 * 
	 * @param i
	 */
	void incCountRequestable(long i);

	/**
	 * removes items from the requsetable
	 * 
	 * @param i
	 */
	void decCountRequestable(long i);

	/**
	 * write to a NBTTagCompound.
	 * 
	 * @param i
	 */
	void writeToNBT(NBTTagCompound i);

	/**
	 * Compare stacks using precise logic.
	 * 
	 * a IAEItemStack to another AEItemStack or a ItemStack.
	 * 
	 * or
	 * 
	 * IAEFluidStack, FluidStack
	 * 
	 * @param obj
	 * @return true if they are the same.
	 */
	@Override
	boolean equals(Object obj);

	/**
	 * compare stacks using fuzzy logic
	 * 
	 * a IAEItemStack to another AEItemStack or a ItemStack.
	 * 
	 * @param st
	 * @param mode
	 * @return true if two stacks are equal based on AE Fuzzy Comparison.
	 */
	boolean fuzzyComparison(Object st, FuzzyMode mode);

	/**
	 * Slower for disk saving, but smaller/more efficient for packets.
	 * 
	 * @param data
	 * @throws IOException
	 */
	void writeToPacket(ByteBuf data) throws IOException;

	/**
	 * Clone the Item / Fluid Stack
	 * 
	 * @return a new Stack, which is copied from the original.
	 */
	StackType copy();

	/**
	 * create an empty stack.
	 * 
	 * @return a new stack, which represents an empty copy of the original.
	 */
	StackType empty();

	/**
	 * obtain the NBT Data for the item.
	 * 
	 * @return
	 */
	IAETagCompound getTagCompound();

	/**
	 * @return true if the stack is a {@link IAEItemStack}
	 */
	boolean isItem();

	/**
	 * @return true if the stack is a {@link IAEFluidStack}
	 */
	boolean isFluid();

	/**
	 * @return ITEM or FLUID
	 */
	StorageChannel getChannel();

}
