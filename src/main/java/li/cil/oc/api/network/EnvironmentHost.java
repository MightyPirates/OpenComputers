package li.cil.oc.api.network;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * To be implemented by 'hosts' of components.
 * <p/>
 * This is what's passed to drivers as the host when creating an environment.
 * It is generally used to represent the components' location in the world.
 * <p/>
 * You will only need to implement this if you intend to host components, e.g.
 * by providing a custom computer case or such. In OpenComputers this interface
 * is usually implemented directly by the tile entities acting as the host, so
 * in most cases you should be able to cast this to <tt>TileEntity</tt> for
 * more options, if necessary.
 */
public interface EnvironmentHost {
    /**
     * The world the host lives in.
     */
    World getWorld();

    /**
     * The host's position in the world.
     * <p/>
     * For tile entities this is the <em>centered</em> position. For example,
     * if the tile entity is located at (0, -2, 3) this will be (0.5, -1.5, 3.5).
     */
    Vec3d getHostPosition();

    /**
     * The host's block position in the world.
     * <p/>
     * For entities this is the <em>containing</em> block position. For example,
     * if the entity is located at (0.3, -2.5, 3.3) this will be (0, -3, 3).
     */
    BlockPos getHostBlockPosition();

    /**
     * Marks the host as "changed" so that it knows it has to be saved again
     * in the next world save. Typically only needed for tile entity hosts,
     * where this should mark the tile entity's chunk as dirty.
     * <p>
     * <em>NB</em>: this may be called from execution threads, so <em>do not</em>
     * directly call {@link TileEntity#markDirty()} in the implementation! Instead,
     * do something like this:
     * <pre>
     * final IThreadListener thread = getWorld().getMinecraftServer();
     * if (thread != null) {
     *     thread.addScheduledTask(this::markDirty);
     * }
     * </pre>
     */
    void markHostChanged();
}
