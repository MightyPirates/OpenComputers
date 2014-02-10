package mods.railcraft.api.carts;

import net.minecraft.entity.item.EntityMinecart;

/**
 * This interface should be implemented by any minecart that wishes
 * to change the default linkage behavior.
 * It is NOT required to be able to link a cart,
 * it merely gives you more control over the process.
 * @author CovertJaguar <http://www.railcraft.info>
 */
public interface ILinkableCart
{

    /**
     * To disable linking altogether, return false here.
     * @return True if this cart is linkable.
     */
    public boolean isLinkable();

    /**
     * Check called when attempting to link carts.
     * @param cart The cart that we are attempting to link with.
     * @return True if we can link with this cart.
     */
    public boolean canLinkWithCart(EntityMinecart cart);

    /**
     * Returns true if this cart has two links
     * or false if it can only link with one cart.
     * @return True if two links
     */
    public boolean hasTwoLinks();

    /**
     * Gets the distance at which this cart can be linked.
     * This is called on both carts and added together to determine
     * how close two carts need to be for a successful link.
     * Default = LinkageManager.LINKAGE_DISTANCE
     * @param cart The cart that you are attempting to link with.
     * @return The linkage distance
     */
    public float getLinkageDistance(EntityMinecart cart);

    /**
     * Gets the optimal distance between linked carts.
     * This is called on both carts and added together to determine
     * the optimal rest distance between linked carts.
     * The LinkageManager will attempt to maintain this distance
     * between linked carts at all times.
     * Default = LinkageManager.OPTIMAL_DISTANCE
     * @param cart The cart that you are linked with.
     * @return The optimal rest distance
     */
    public float getOptimalDistance(EntityMinecart cart);

    /**
     * Return false if linked carts have no effect on the velocity of this cart.
     * Use carefully, if you link two carts that can't be adjusted,
     * it will behave as if they are not linked.
     * @param cart The cart doing the adjusting.
     * @return Whether the cart can have its velocity adjusted.
     */
    public boolean canBeAdjusted(EntityMinecart cart);

    /**
     * Called upon successful link creation.
     * @param cart The cart we linked with.
     */
    public void onLinkCreated(EntityMinecart cart);

    /**
     * Called when a link is broken (usually).
     * @param cart The cart we were linked with.
     */
    public void onLinkBroken(EntityMinecart cart);
}
