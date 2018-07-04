package li.cil.oc.api.event;

import li.cil.oc.api.internal.Agent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.Cancelable;

public abstract class RobotBreakBlockEvent extends RobotEvent {
    protected RobotBreakBlockEvent(Agent agent) {
        super(agent);
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
        public final BlockPos pos;

        /**
         * The time it takes to break the block.
         */
        private double breakTime;

        public Pre(Agent agent, World world, BlockPos pos, double breakTime) {
            super(agent);
            this.world = world;
            this.pos = pos;
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

        public Post(Agent agent, double experience) {
            super(agent);
            this.experience = experience;
        }
    }
}
