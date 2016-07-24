package li.cil.oc.api.driver;

import li.cil.oc.api.network.ManagedEnvironment;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Interface for block component drivers.
 * <p/>
 * This driver type is used for components that are blocks, i.e. that can be
 * placed in the world, but cannot be modified to or don't want to have their
 * {@link net.minecraft.tileentity.TileEntity} implement one of the interfaces
 * for environments ({@link li.cil.oc.api.network.Environment} or
 * {@link li.cil.oc.api.network.SidedEnvironment}).
 * <p/>
 * A block driver is used by <tt>Adapter</tt> blocks to check its neighbors and
 * whether those neighbors should be treated as components or not. If a driver
 * is present, it will be used to create a {@link ManagedEnvironment} that is
 * managed by the adapter.
 * <p/>
 * Note that it is possible to write one driver that supports as many different
 * blocks as you wish. I'd recommend writing one per device (type), though, to
 * keep things modular.
 *
 * @deprecated Use {@link SidedBlock} instead, ignoring the side argument if
 * the side doesn't matter.
 */
@Deprecated // TODO Remove in OC 1.7
public interface Block {
    /**
     * Used to determine the block types this driver handles.
     * <p/>
     * This is used to determine which driver to use for a block placed next to
     * an <tt>Adapter</tt> block. Note that the return value should not change
     * over time; if it does, though, an already installed component will not
     * be removed, since this value is only checked when scanning blocks. You
     * can force this by sending a neighbor block change notification.
     *
     * @param world the world in which the block to check lives.
     * @param pos   the position coordinate of the block to check.
     * @return <tt>true</tt> if the block is supported; <tt>false</tt> otherwise.
     */
    boolean worksWith(World world, BlockPos pos);

    /**
     * Create a new managed environment interfacing the specified block.
     * <p/>
     * This is used to connect the component to the component network when it
     * is detected next to an <tt>Adapter</tt>. Components that are not part of
     * the component network probably don't make much sense (can't think of any
     * uses at this time), but you may still opt to not implement this - i.e.
     * it is safe to return <tt>null</tt> here.
     * <p/>
     * This is expected to return a <em>new instance</em> each time it is
     * called. The created instance's life cycle is managed by the
     * <tt>Adapter</tt> block that caused its creation.
     *
     * @param world the world containing the block to get the environment for.
     * @param pos   the position of the block to get the environment for.
     * @return the environment for the block at that location.
     */
    ManagedEnvironment createEnvironment(World world, BlockPos pos);
}