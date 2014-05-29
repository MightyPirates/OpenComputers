package li.cil.oc.api.event;

import cpw.mods.fml.common.eventhandler.Cancelable;
import li.cil.oc.api.machine.Robot;
import net.minecraftforge.common.util.ForgeDirection;

public abstract class RobotMoveEvent extends RobotEvent {
    /**
     * The direction in which the robot will be moving.
     */
    public final ForgeDirection direction;

    protected RobotMoveEvent(Robot robot, ForgeDirection direction) {
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
        public Pre(Robot robot, ForgeDirection direction) {
            super(robot, direction);
        }
    }

    /**
     * Fired after a robot moved.
     */
    public static class Post extends RobotMoveEvent {
        public Post(Robot robot, ForgeDirection direction) {
            super(robot, direction);
        }
    }
}
