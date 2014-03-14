package mods.railcraft.api.carts;

import net.minecraft.entity.item.EntityMinecart;

/**
 * The LinkageManager contains all the functions needed to link and interact
 * with linked carts.
 *
 * To obtain an instance of this interface, call CartTools.getLinkageManager().
 *
 * Each cart can up to two links. They are called Link A and Link B.
 * Some carts will have only Link A, for example the Tunnel Bore.
 *
 * @author CovertJaguar <http://www.railcraft.info>
 * @see CartTools, ILinkableCart
 */
public interface ILinkageManager
{

    /**
     * The default max distance at which carts can be linked, divided by 2.
     */
    public static final float LINKAGE_DISTANCE = 1.25f;
    /**
     * The default distance at which linked carts are maintained, divided by 2.
     */
    public static final float OPTIMAL_DISTANCE = 0.78f;

    /**
     * Creates a link between two carts,
     * but only if there is nothing preventing such a link.
     *
     * @param cart1
     * @param cart2
     * @return True if the link succeeded.
     */
    public boolean createLink(EntityMinecart cart1, EntityMinecart cart2);

    /**
     * Returns the cart linked to Link A or null if nothing is currently
     * occupying Link A.
     *
     * @param cart The cart for which to get the link
     * @return The linked cart or null
     */
    public EntityMinecart getLinkedCartA(EntityMinecart cart);

    /**
     * Returns the cart linked to Link B or null if nothing is currently
     * occupying Link B.
     *
     * @param cart The cart for which to get the link
     * @return The linked cart or null
     */
    public EntityMinecart getLinkedCartB(EntityMinecart cart);

    /**
     * Returns true if the two carts are linked to each other.
     *
     * @param cart1
     * @param cart2
     * @return True if linked
     */
    public boolean areLinked(EntityMinecart cart1, EntityMinecart cart2);

    /**
     * Breaks a link between two carts, if any link exists.
     *
     * @param cart1
     * @param cart2
     */
    public void breakLink(EntityMinecart cart1, EntityMinecart cart2);

    /**
     * Breaks all links the cart has.
     *
     * @param cart
     */
    public void breakLinks(EntityMinecart cart);

    /**
     * Break only link A.
     *
     * @param cart
     */
    public void breakLinkA(EntityMinecart cart);

    /**
     * Break only link B.
     *
     * @param cart
     */
    public void breakLinkB(EntityMinecart cart);

    /**
     * Counts how many carts are in the train.
     *
     * @param cart Any cart in the train
     * @return The number of carts in the train
     */
    public int countCartsInTrain(EntityMinecart cart);
}
