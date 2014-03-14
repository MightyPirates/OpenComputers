package appeng.api;

import java.io.DataOutputStream;
import java.io.IOException;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * An alternate version of ItemStack for AE to keep tabs on things easier, and to support larger storage.
 * stackSizes of getItemStack will be capped.
 * 
 * You may hold on to these if you want, just make sure you let go of them when your not using them.
 * 
 * Don't Implement.
 * 
 * Construct with Util.createItemStack( ItemStack )
 * 
 */
public interface IAEItemStack
{
	/**
	 * seriously?
	 * @return the item id of the requested item.
	 */
	public int getItemID();
	
	/**
	 * the item's damage.
	 * @return
	 */
	public int getItemDamage();
	
	/**
	 * obtain the NBT Data for the item.
	 * @return
	 */
	public IAETagCompound getTagCompound();
	
	/**
	 * creates a standard MC ItemStack for the item.
	 * @return new itemstack
	 */
	public ItemStack getItemStack();
	
	/**
	 * create a AE Item clone.
	 * @return the copy.
	 */
	public IAEItemStack copy();
	
	/**
	 * number of items in the stack.
	 * @return basically ItemStack.stackSize
	 */
	public long getStackSize();
	
	/**
	 * changes the number of items in the stack.
	 * @param basically, ItemStack.stackSize = N
	 */
	public void setStackSize( long stackSize );
	
	/**
	 * Same as getStackSize, but for requestable items. ( LP )
	 * @return basically itemStack.stackSize but for requestable items.
	 */
	long getCountRequestable();

	/**
	 * Same as setStackSize, but for requestable items. ( LP )
	 * @return basically itemStack.stackSize = N but for setStackSize items.
	 */
	void setCountRequestable(long countRequestable);
	
	/**
	 * true, if the item can be crafted.
	 * @return true, if it can be crafted.
	 */
	boolean isCraftable();
	
	/**
	 * change weather the item can be crafted.
	 * @param isCraftable
	 */
	void setCraftable(boolean isCraftable);
	
	/**
	 * basically itemid and damage in one number..
	 * @return
	 */
	int getDef();
	
	/**
	 * clears, requsetable, craftable, and stack sizes.
	 */
	public void reset();
	
	/**
	 * returns true, if the item can be crafted, requested, or extracted.
	 * @return isThisRecordMeaningful
	 */
	boolean isMeaninful();
	
	/**
	 * is there NBT Data for this item?
	 * @return if there is.
	 */
	boolean hasTagCompound();
	
	/**
	 * Combines two IAEItemStacks via addition.
	 * @param option, to add to the current one.
	 */
	void add(IAEItemStack option);
	
	/**
	 * Adds more to the stack size...
	 * @param i
	 */
	void incStackSize(long i);
	
	/**
	 * removes some from the stack size.
	 */
	void decStackSize(long i);
	
	/**
	 * adds items to the requestable
	 * @param i
	 */
	void incCountRequestable(long i);
	
	/**
	 * removes items from the requsetable
	 * @param i
	 */
	void decCountRequestable(long i);
	
	/**
	 * quick way to get access to the MC Item Definition.
	 * @return
	 */
	Item getItem();
	
	/**
	 * write to a NBTTagCompound.
	 * @param i
	 */
	void writeToNBT(NBTTagCompound i);
	
	/**
	 * Compare a IAEItemStack to another AEItemStack or a ItemStack.
	 * @param obj
	 * @return true if they are the same.
	 */
	@Override
	public boolean equals(Object obj);

	/**
	 * Compare the Ore Dictionary ID for this to another item.
	 * @param oreID 
	 * @return
	 */
	public boolean sameOre( Object oreID );
	
	/**
	 * Slower for disk saving, but smaller/more efficient for packets.
	 * @param data
	 * @throws IOException 
	 */
	public void writeToPacket(DataOutputStream data) throws IOException;
	
}
