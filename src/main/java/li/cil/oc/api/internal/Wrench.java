package li.cil.oc.api.internal;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
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
     * @param pos      the position of the block.
     * @param simulate whether to simulate the usage.
     * @return whether the wrench can be used on the block.
     */
    boolean useWrenchOnBlock(EntityPlayer player, World world, BlockPos pos, boolean simulate);
}
