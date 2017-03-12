package li.cil.oc.api.network;

import li.cil.oc.api.util.Location;
import net.minecraft.tileentity.TileEntity;

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
public interface NodeContainerHost extends Location {
    /**
     * Marks the host as "changed" so that it knows it has to be saved again
     * in the next world save. Typically only needed for tile entity hosts,
     * where this should mark the tile entity's chunk as dirty.
     * <p>
     * <em>NB</em>: this may be called from execution threads, so <em>do not</em>
     * directly call {@link TileEntity#markDirty()} in the implementation! Instead,
     * do something like this:
     * <pre>
     * private final Object lock = new Object();
     * private boolean isMarkDirtyScheduled;
     *
     * &#064;Override
     * public void markHostChanged() {
     *     final IThreadListener thread = getWorld().getMinecraftServer();
     *     if (thread != null) {
     *         synchronized (lock) {
     *             if (!isMarkDirtyScheduled) {
     *                 isMarkDirtyScheduled = true;
     *                 thread.addScheduledTask(this::markDirty);
     *             }
     *         }
     *     }
     * }
     *
     * &#064;Override
     * public void markDirty() {
     *     isMarkDirtyScheduled = false;
     *     super.markDirty();
     * }
     * </pre>
     */
    void markHostChanged();
}
