package mods.railcraft.api.tracks;

import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import mods.railcraft.api.core.items.ITrackItem;
import net.minecraft.block.BlockRailBase;

/**
 * A number of utility functions related to rails.
 *
 * @author CovertJaguar <http://www.railcraft.info>
 */
public abstract class RailTools {

    /**
     * Attempts to place a rail of the type provided. There is no need to verify
     * that the ItemStack contains a valid rail prior to calling this function.
     *
     * The function takes care of that and will return false if the ItemStack is
     * not a valid ITrackItem or an ItemBlock who's id will return true when
     * passed to BlockRailBase.isRailBlock(itemID).
     *
     * That means this function can place any Railcraft or vanilla rail and has
     * at least a decent chance of being able to place most third party rails.
     *
     * @param stack The ItemStack containing the rail
     * @param world The World object
     * @param i x-Coord
     * @param j y-Coord
     * @param k z-Coord
     * @return true if successful
     * @see ITrackItem
     */
    public static boolean placeRailAt(ItemStack stack, World world, int i, int j, int k) {
        if (stack == null) {
            return false;
        }
        if (stack.getItem() instanceof ITrackItem) {
            return ((ITrackItem) stack.getItem()).placeTrack(stack.copy(), world, i, j, k);
        }
        if (stack.getItem() instanceof ItemBlock && stack.itemID < Block.blocksList.length && BlockRailBase.isRailBlock(stack.itemID)) {
            boolean success = world.setBlock(i, j, k, stack.itemID);
            if (success) {
                world.playSoundEffect((float) i + 0.5F, (float) j + 0.5F, (float) k + 0.5F, Block.rail.stepSound.getStepSound(), (Block.rail.stepSound.getVolume() + 1.0F) / 2.0F, Block.rail.stepSound.getPitch() * 0.8F);
            }
            return success;
        }
        return false;
    }

    /**
     * Returns true if the ItemStack contains a valid Railcraft Track item.
     *
     * Will return false is passed a vanilla rail.
     *
     * @param stack The ItemStack to test
     * @return true if rail
     * @see ITrackItem
     */
    public static boolean isTrackItem(ItemStack stack) {
        return stack != null && stack.getItem() instanceof ITrackItem;
    }

    /**
     * Checks to see if a cart is being held by a ITrackLockdown.
     *
     * @param cart The cart to check
     * @return True if being held
     */
    public static boolean isCartLockedDown(EntityMinecart cart) {
        int x = MathHelper.floor_double(cart.posX);
        int y = MathHelper.floor_double(cart.posY);
        int z = MathHelper.floor_double(cart.posZ);

        if (BlockRailBase.isRailBlockAt(cart.worldObj, x, y - 1, z)) {
            y--;
        }

        TileEntity tile = cart.worldObj.getBlockTileEntity(x, y, z);
        if (tile instanceof ITrackTile) {
            ITrackInstance track = ((ITrackTile) tile).getTrackInstance();
            return track instanceof ITrackLockdown && ((ITrackLockdown) track).isCartLockedDown(cart);
        } else if (tile instanceof ITrackLockdown) {
            return ((ITrackLockdown) tile).isCartLockedDown(cart);
        }
        return false;
    }
}
