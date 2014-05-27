package li.cil.oc.api.driver;

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
public interface Container {
    /**
     * The world the container lives in.
     */
    World world();

    /**
     * The container's X position in the world.
     * <p/>
     * For tile entities this is the <em>centered</em> position. For example,
     * if the tile entity is located at (0, 2, 3) this will be 0.5.
     */
    double xPosition();

    /**
     * The container's Y position in the world.
     * <p/>
     * For tile entities this is the <em>centered</em> position. For example,
     * if the tile entity is located at (0, 2, 3) this will be 2.5.
     */
    double yPosition();

    /**
     * The container's Z position in the world.
     * <p/>
     * For tile entities this is the <em>centered</em> position. For example,
     * if the tile entity is located at (0, 2, 3) this will be 3.5.
     */
    double zPosition();

    /**
     * Marks the container as "changed" so that it knows it has to be saved
     * again in the next world save.
     */
    void markChanged();
}
