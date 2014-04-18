package appeng.api.storage.data;

import java.util.Collection;
import java.util.Iterator;

import appeng.api.config.FuzzyMode;

/**
 * Represents a list of items in AE.
 * 
 * Don't Implement.
 * 
 * Construct with Util.createItemList()
 */
public interface IItemList<StackType extends IAEStack> extends Iterable<StackType>
{

	/**
	 * add a stack to the list stackSize is used to add to stackSize, this will merge the stack with an item already in
	 * the list if found.
	 * 
	 * @param option
	 */
	public void addStorage(StackType option); // adds a stack as stored

	/**
	 * add a stack to the list as craftable, this will merge the stack with an item already in the list if found.
	 * 
	 * @param option
	 */
	public void addCrafting(StackType option);

	/**
	 * add a stack to the list, stack size is used to add to requstable, this will merge the stack with an item already
	 * in the list if found.
	 * 
	 * @param option
	 */
	public void addRequestable(StackType option); // adds a stack as requestable

	/**
	 * add a stack to the list, this will merge the stack with an item already in the list if found.
	 * 
	 * @param option
	 */
	public void add(StackType option); // adds stack as is

	/**
	 * @return the first item in the list
	 */
	StackType getFirstItem();

	/**
	 * @param i
	 * @return a stack equivalent to the stack passed in, but with the correct stack size information, or null if its
	 *         not present
	 */
	StackType findPrecise(StackType i);

	/**
	 * @param input
	 * @return a list of relevant fuzzy matched stacks
	 */
	public Collection<StackType> findFuzzy(StackType input, FuzzyMode fuzzy);

	/**
	 * @return the number of items in the list
	 */
	int size();

	/**
	 * allows you to iterate the list.
	 */
	@Override
	public Iterator<StackType> iterator();

	/**
	 * @return true if there are no items in the list
	 */
	public boolean isEmpty();

	/**
	 * resets stack sizes to 0.
	 */
	void resetStatus();

}