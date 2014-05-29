package li.cil.oc.api.event;

import cpw.mods.fml.common.eventhandler.Cancelable;
import li.cil.oc.api.machine.Robot;
import net.minecraft.world.World;

public abstract class RobotBreakBlockEvent extends RobotEvent {
    protected RobotBreakBlockEvent(Robot robot) {
        super(robot);
    }

    /**
     * Fired when a robot is about to break a block.
     * <p/>
     * Canceling this event will prevent the block from getting broken.
     */
    @Cancelable
    public static class Pre extends RobotBreakBlockEvent {
        /**
         * The world in which the block will be broken.
         */
        public final World world;

        /**
         * The coordinates at which the block will be broken.
         */
        public final int x, y, z;

        /**
         * The time it takes to break the block.
         */
        private double breakTime;

        public Pre(Robot robot, World world, int x, int y, int z, double breakTime) {
            super(robot);
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.breakTime = breakTime;
        }

        /**
         * Sets the time it should take the robot to break the block.
         * <p/>
         * Note that the robot will still break the block instantly, but the
         * robot's execution is paused for the specified amount of time.
         *
         * @param breakTime the time in seconds the break operation takes.
         */
        public void setBreakTime(double breakTime) {
            this.breakTime = Math.max(0.05, breakTime);
        }

        /**
         * Gets the time that it will take to break the block.
         *
         * @see #setBreakTime(double)
         */
        public double getBreakTime() {
            return breakTime;
        }
    }

    /**
     * Fired after a robot broke a block.
     */
    public static class Post extends RobotBreakBlockEvent {
        /**
         * The amount of experience the block that was broken generated (e.g. certain ores).
         */
        public final double experience;

        public Post(Robot robot, double experience) {
            super(robot);
            this.experience = experience;
        }
    }
}
