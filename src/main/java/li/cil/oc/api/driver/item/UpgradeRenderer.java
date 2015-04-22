package li.cil.oc.api.driver.item;

import li.cil.oc.api.event.RobotRenderEvent;
import li.cil.oc.api.internal.Robot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * This interface can be implemented by items to allow custom rendering of
 * upgrades installed in robots.
 * <p/>
 * Upgrades installed in a robot can have an external representation. This is
 * achieved by implementing this interface on an item that serves as a
 * renderable upgrade. When the robot is rendered, each equipped upgrade is
 * checked for this interface, and if present, the {@link #render} method
 * is called.
 */
@SideOnly(Side.CLIENT)
public interface UpgradeRenderer {
    /**
     * The priority with which to render this upgrade.
     * <p/>
     * Upgrades with a higher priority are preferred for rendering over other
     * upgrades. Upgrades with higher priorities will be rendered in the top
     * slots, i.e. the assigned slot order is top then bottom.
     * <p/>
     * You usually won't need the robot parameter, but in case you <em>do</em>
     * need some contextual information, this should provide you with anything
     * you could need.
     *
     * @param stack the item stack of the upgrade to render.
     * @param robot the robot the upgrade is rendered on.
     * @return the priority with which to render the upgrade.
     */
    int priority(ItemStack stack, Robot robot);

    /**
     * Whether the upgrade can be rendered in the specified mount point.
     * <p/>
     * This is used to determine whether an upgrade can be rendered in a
     * specific mount point, or not. Note that if the upgrade refuses to
     * be rendered in the offered mount point, it will not be rendered at all,
     * i.e. it will not be offered another mount point. To give the upgrade
     * a better chance to get a usable mount point, specify an appropriate
     * priority via {@link #priority}.
     *
     * @param stack      the item stack of the upgrade to render.
     * @param mountPoint the mount-point to render the upgrade at.
     * @param robot      the robot the upgrade is rendered on.
     * @return whether the upgrade can be rendered in the specified mount point.
     */
    boolean canRender(ItemStack stack, RobotRenderEvent.MountPoint mountPoint, Robot robot);

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
     * {@link #canRender}, otherwise it will still consume a mount point.
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
}
