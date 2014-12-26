package li.cil.oc.api.event;

import net.minecraftforge.fml.common.eventhandler.Cancelable;
import li.cil.oc.api.internal.Robot;
import net.minecraft.util.EnumFacing;

public abstract class RobotMoveEvent extends RobotEvent {
    /**
     * The direction in which the robot will be moving.
     */
    public final EnumFacing direction;

    protected RobotMoveEvent(Robot robot, EnumFacing direction) {
        super(robot);
        this.direction = direction;
    }

    /**
     * Fired when a robot is about to move.
     * <p/>
     * Canceling the event will prevent the robot from moving.
     */
    @Cancelable
    public static class Pre extends RobotMoveEvent {
        public Pre(Robot robot, EnumFacing direction) {
            super(robot, direction);
        }
    }

    /**
     * Fired after a robot moved.
     */
    public static class Post extends RobotMoveEvent {
        public Post(Robot robot, EnumFacing direction) {
            super(robot, direction);
        }
    }
}
