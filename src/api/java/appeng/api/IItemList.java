package appeng.api;

import java.util.Iterator;
import java.util.List;

import net.minecraft.item.ItemStack;

/**
 * Represents a list of items in AE.
 * 
 * Don't Implement.
 * 
 * Construct with Util.createItemList()
 * 
 */
public interface IItemList extends Iterable<IAEItemStack>
{
	public void addStorage( IAEItemStack option ); // adds a stack as stored.
	public void addCrafting( IAEItemStack option ); // adds a stack as craftable.
	public void addRequestable( IAEItemStack option ); // adds a stack as requestable.
	public void add( IAEItemStack option ); // adds stack as is.
	
	IAEItemStack getFirstItem();
	
	public List<ItemStack> getItems();
	IAEItemStack findItem(IAEItemStack i);
	int size();
	
	@Override
	public Iterator<IAEItemStack> iterator();
	public void setCurrentPriority(int priority);
}
