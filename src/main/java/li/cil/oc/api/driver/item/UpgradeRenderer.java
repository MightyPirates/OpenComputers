package li.cil.oc.api.driver.item;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import li.cil.oc.api.event.RobotRenderEvent;
import li.cil.oc.api.internal.Robot;
import net.minecraft.item.ItemStack;

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
public interface UpgradeRenderer {
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
     * If the stack cannot be rendered, simply do nothing. This way it's fine
     * to implement this on a meta item.
     * <p/>
     * You usually won't need the robot parameter, but in case you <em>do</em>
     * need some contextual information, this should provide you with anything
     * you could need.
     *
     * @param stack      the item stack of the upgrade to render.
     * @param mountPoint the mount-point to render the upgrade at.
     * @param robot      the robot the upgrade is rendered on.
     */
    @SideOnly(Side.CLIENT)
    void render(ItemStack stack, RobotRenderEvent.MountPoint mountPoint, Robot robot);
}
