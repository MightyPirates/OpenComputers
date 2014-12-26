package li.cil.oc.api.event;

import net.minecraft.util.BlockPos;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import li.cil.oc.api.internal.Robot;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public abstract class RobotPlaceBlockEvent extends RobotEvent {
    /**
     * The item that is used to place the block.
     */
    public final ItemStack stack;

    /**
     * The world in which the block will be placed.
     */
    public final World world;

    /**
     * The coordinates at which the block will be placed.
     */
    public final BlockPos pos;

    protected RobotPlaceBlockEvent(Robot robot, ItemStack stack, World world, BlockPos pos) {
        super(robot);
        this.stack = stack;
        this.world = world;
        this.pos = pos;
    }

    /**
     * Fired when a robot is about to place a block.
     * <p/>
     * Canceling this event will prevent the block from being placed.
     */
    @Cancelable
    public static class Pre extends RobotPlaceBlockEvent {
        public Pre(Robot robot, ItemStack stack, World world, BlockPos pos) {
            super(robot, stack, world, pos);
        }
    }

    /**
     * Fired after a robot placed a block.
     */
    public static class Post extends RobotPlaceBlockEvent {
        public Post(Robot robot, ItemStack stack, World world, BlockPos pos) {
            super(robot, stack, world, pos);
        }
    }
}
