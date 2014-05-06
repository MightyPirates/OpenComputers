package li.cil.oc.api.event;

import li.cil.oc.api.machine.Robot;
import net.minecraft.util.Vec3;
import net.minecraftforge.event.Cancelable;

/**
 * Fired directly before the robot's chassis is rendered.
 * <p/>
 * If this event is canceled, the chassis will <em>not</em> be rendered.
 * Component items' item renderes will still be invoked, at the possibly
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
         */
        public final Vec3 offset = Vec3.createVectorHelper(0, 0, 0);

        /**
         * The vector the mount point is facing.
         */
        public final Vec3 normal = Vec3.createVectorHelper(0, 0, 0);
    }
}
