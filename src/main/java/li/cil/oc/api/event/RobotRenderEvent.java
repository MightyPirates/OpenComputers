package li.cil.oc.api.event;

import cpw.mods.fml.common.eventhandler.Cancelable;
import li.cil.oc.api.internal.Robot;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

/**
 * Fired directly before the robot's chassis is rendered.
 * <p/>
 * If this event is canceled, the chassis will <em>not</em> be rendered.
 * Component items' item renderers will still be invoked, at the possibly
 * modified mount points.
 * <p/>
 * <em>Important</em>: the robot instance may be null in this event, in
 * case the render pass is for rendering the robot in an inventory.
 */
@Cancelable
public class RobotRenderEvent extends RobotEvent {
    /**
     * Points on the robot at which component models may be rendered.
     * <p/>
     * By convention, components should be rendered in order of their slots,
     * meaning that some components may not be rendered at all, if there are
     * not enough mount points.
     * <p/>
     * The equipped tool is rendered at a fixed position, this list does not
     * contain a mount point for it.
     */
    public final MountPoint[] mountPoints;

    public RobotRenderEvent(Robot robot, MountPoint[] mountPoints) {
        super(robot);
        this.mountPoints = mountPoints;
    }

    /**
     * Describes points on the robot model at which components are "mounted",
     * i.e. where component models may be rendered.
     */
    public static class MountPoint {
        /**
         * The position of the mount point, relative to the robot's center.
         * For the purposes of this offset, the robot is always facing south,
         * i.e. the positive Z axis is 'forward'.
         * <p/>
         * Note that the rotation is applied <em>before</em> the translation.
         */
        public final Vector3f offset = new Vector3f(0, 0, 0);

        /**
         * The orientation of the mount point specified by the angle and the
         * vector to rotate around. The rotation is applied in one
         * GL11.glRotate() call. Note that the <tt>W</tt> component of the
         * vector is the rotation.
         * <p/>
         * Note that the rotation is applied <em>before</em> the translation.
         */
        public final Vector4f rotation = new Vector4f(0, 0, 0, 0);
    }
}
