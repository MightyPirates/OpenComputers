package mods.railcraft.api.carts;

import net.minecraft.item.ItemStack;
import mods.railcraft.api.core.items.IStackFilter;

/**
 * This interface allows items to be passed around with out needing
 * to know anything about the underlying implementation of the inventories.
 *
 * @author CovertJaguar <http://www.railcraft.info>
 */
public interface IItemTransfer
{

    /**
     * Offers an ItemStack to the object implementing this interface.
     * This function will return null if the item is accepted in full,
     * otherwise it will return whatever is rejected.
     *
     * @param source The Object offering the item
     * @param offer The ItemStack being offered
     * @return Unused or unwanted portions of offer
     */
    public ItemStack offerItem(Object source, ItemStack offer);

    /**
     * Requests an ItemStack from the object implementing this interface.
     * It is up to the object implementing this interface to determine which
     * ItemStack to return, or none at all.
     *
     * @param source The Object submitting the request
     * @return An ItemStack to fulfill the request or null if refused.
     */
    public ItemStack requestItem(Object source);

    /**
     * Requests an ItemStack from the object implementing this interface
     * that matches the request parameter.
     * It is up to the object implementing this interface to
     * determine which ItemStack to return, or none at all.
     * However, if the return value is not null
     * it should fulfill the following condition:<br/>
     * InvTools.isItemEqual(it.requestItem(this,request), request) == true
     *
     * @param source The Object submitting the request
     * @param request The type of item requested
     * @return An ItemStack to fulfill the request or null if refused.
     */
    public ItemStack requestItem(Object source, ItemStack request);

    /**
     * Requests an ItemStack from the object implementing this interface
     * that matches the request parameter.
     * It is up to the object implementing this interface to
     * determine which ItemStack to return, or none at all.
     * However, if the return value is not null
     * it should fulfill the following condition:<br/>
     * IStackFilter.matches(it.requestItem(this,request), request) == true
     *
     * @param source The Object submitting the request
     * @param request The type of item requested
     * @return An ItemStack to fulfill the request or null if refused.
     */
    public ItemStack requestItem(Object source, IStackFilter request);
}
