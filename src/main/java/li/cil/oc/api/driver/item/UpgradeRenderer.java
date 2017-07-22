package li.cil.oc.api.driver.item;

import li.cil.oc.api.event.RobotRenderEvent;
import li.cil.oc.api.internal.Robot;
import net.minecraft.item.ItemStack;

import java.util.Set;

/**
 * This interface can be implemented by items to allow custom rendering of
 * upgrades installed in robots.
 * <p/>
 * Upgrades installed in a robot can have an external representation. This is
 * achieved by implementing this interface on an item that serves as a
 * renderable upgrade. When the robot is rendered, each equipped upgrade is
 * checked for this interface, and if present, the {@link #render} method
 * is called.
 * <p/>
 * Note that these methods are only ever called on the client side.
 */
public interface UpgradeRenderer {
    /**
     * Returns which mount point this renderer wants to render the specified
     * upgrade in.
     * <p/>
     * This method is used to determine which upgrade is rendered where, and is
     * called for every installed, renderable upgrade. The available mount
     * point names are defined in {@link MountPointName}, with the two special
     * values <tt>None</tt> and <tt>Any</tt>.
     * <p/>
     * <tt>None</tt> means that the upgrade should not be rendered at all. This
     * can be the case when there is no slot remaining that the upgrade may be
     * rendered in. Returning <tt>null</tt> is equivalent to returning <tt>None</tt>.
     * <p/>
     * <tt>Any</tt> means that the upgrade doesn't really care where it's being
     * rendered. Mount points not assigned by another upgrade preferring to be
     * rendered in it will be assigned to such upgrades in the order they are
     * installed in the robot.
     * <p/>
     * Returning a mount point not in the list of available mount points will
     * be equivalent to returning <tt>None</tt>.
     *
     * @param stack                the item stack of the upgrade to render.
     * @param robot                the robot the upgrade is rendered on.
     * @param availableMountPoints the mount points available for rendering in.
     * @return the mount point to reserve for the upgrade.
     */
    String computePreferredMountPoint(ItemStack stack, Robot robot, Set<String> availableMountPoints);

    /**
     * Render the specified upgrade on a robot.
     * <p/>
     * The GL state has not been adjusted to the mount points position, so
     * that you can perform rotations without having to revert the translation.
     * It is your responsibility to position the rendered model to fit the
     * specified mount point. The state will be such that the origin is the
     * center of the robot. This is what the offset of the mount-point is
     * relative to.
     * <p/>
     * If the stack cannot be rendered, the renderer should indicate so in
     * {@link #computePreferredMountPoint}, otherwise it will still consume a mount
     * point.
     * <p/>
     * You usually won't need the robot parameter, but in case you <em>do</em>
     * need some contextual information, this should provide you with anything
     * you could need.
     *
     * @param stack      the item stack of the upgrade to render.
     * @param mountPoint the mount-point to render the upgrade at.
     * @param robot      the robot the upgrade is rendered on.
     * @param pt         partial tick time, e.g. for animations.
     */
    void render(ItemStack stack, RobotRenderEvent.MountPoint mountPoint, Robot robot, float pt);

    /**
     * Mount point names for {@link #computePreferredMountPoint}.
     */
    final class MountPointName {
        public static final String None = "none";
        public static final String Any = "any";

        public static final String TopLeft = "top_left";
        public static final String TopRight = "top_right";
        public static final String TopBack = "top_back";
        public static final String BottomLeft = "bottom_left";
        public static final String BottomRight = "bottom_right";
        public static final String BottomBack = "bottom_back";
        public static final String BottomFront = "bottom_front";

        private MountPointName() {
        }
    }
}
