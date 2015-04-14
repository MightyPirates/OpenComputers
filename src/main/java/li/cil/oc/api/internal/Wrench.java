package li.cil.oc.api.internal;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

/**
 * Implemented on items that are wrench-like tools.
 */
public interface Wrench {
    /**
     * Called when the wrench is used.
     * <p/>
     * This is called in two scenarios, once when testing whether the wrench
     * can be used on a certain block, in which case the <tt>simulate</tt>
     * argument will be <tt>true</tt>, and once when actually used on a block,
     * in which case the <tt>simulate</tt> argument will be <tt>false</tt>,
     * allowing the tool to damage itself, for example.
     * <p/>
     * This is usually called from blocks' activation logic.
     *
     * @param player   the player using the tool
     * @param world    the world containing the block the wrench is used on.
     * @param x        the X coordinate of the block.
     * @param y        the Y coordinate of the block.
     * @param z        the Z coordinate of the block.
     * @param simulate whether to simulate the usage.
     * @return whether the wrench can be used on the block.
     */
    boolean useWrenchOnBlock(EntityPlayer player, World world, int x, int y, int z, boolean simulate);
}
