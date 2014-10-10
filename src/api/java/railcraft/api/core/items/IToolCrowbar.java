/*
 * This code is the property of CovertJaguar
 * and may only be used with explicit written
 * permission unless otherwise specified on the
 * license page at railcraft.wikispaces.com.
 */
package mods.railcraft.api.core.items;

import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

/**
 *
 * @author CovertJaguar <http://www.railcraft.info>
 */
public interface IToolCrowbar {

    /**
     * Controls non-rotational interactions with blocks. Crowbar specific stuff.
     *
     * Rotational interaction is handled by the Block.rotateBlock() function,
     * which should be called from the Item.onUseFirst() function of your tool.
     *
     * @param player
     * @param crowbar
     * @param x
     * @param y
     * @param z
     * @return
     */
    public boolean canWhack(EntityPlayer player, ItemStack crowbar, int x, int y, int z);

    /**
     * Callback to do damage to the item.
     *
     * @param player
     * @param crowbar
     * @param x
     * @param y
     * @param z
     */
    public void onWhack(EntityPlayer player, ItemStack crowbar, int x, int y, int z);

    /**
     * Controls whether you can link a cart.
     *
     * @param player
     * @param crowbar
     * @param cart
     * @return
     */
    public boolean canLink(EntityPlayer player, ItemStack crowbar, EntityMinecart cart);

    /**
     * Callback to do damage.
     *
     * @param player
     * @param crowbar
     * @param cart
     */
    public void onLink(EntityPlayer player, ItemStack crowbar, EntityMinecart cart);

    /**
     * Controls whether you can boost a cart.
     *
     * @param player
     * @param crowbar
     * @param cart
     * @return
     */
    public boolean canBoost(EntityPlayer player, ItemStack crowbar, EntityMinecart cart);

    /**
     * Callback to do damage, boosting a cart usually does more damage than
     * normal usage.
     *
     * @param player
     * @param crowbar
     * @param cart
     */
    public void onBoost(EntityPlayer player, ItemStack crowbar, EntityMinecart cart);
}
