package mods.railcraft.api.core.items;

import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * This interface should be implemented by any cart item,
 * but it is generally optional.
 *
 * @author CovertJaguar <http://www.railcraft.info>
 */
public interface IMinecartItem
{

    /**
     * Controls whether this cart item can be placed by the Cart and Train Dispensers.
     *
     * Generally, you can ignore the placeCart() function if this returns false.
     *
     * @return true if it can be placed, false otherwise
     */
    public boolean canBePlacedByNonPlayer(ItemStack cart);

    /**
     * Places a cart at the specified location.
     *
     * Implementing this function is optional.
     *
     * @param owner the name of the player placing the cart or "[MyMod]" with the brackets
     * @param cart An ItemStack that contains the cart
     * @param world The World
     * @param i x-Coord
     * @param j y-Coord
     * @param k z-Coord
     * @return the cart placed or null if failed
     */
    public EntityMinecart placeCart(String owner, ItemStack cart, World world, int i, int j, int k);
}
