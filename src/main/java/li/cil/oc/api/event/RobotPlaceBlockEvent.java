package li.cil.oc.api.event;

import li.cil.oc.api.machine.Robot;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.event.Cancelable;

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
    public final int x, y, z;

    protected RobotPlaceBlockEvent(Robot robot, ItemStack stack, World world, int x, int y, int z) {
        super(robot);
        this.stack = stack;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Fired when a robot is about to place a block.
     * <p/>
     * Canceling this event will prevent the block from being placed.
     */
    @Cancelable
    public static class Pre extends RobotPlaceBlockEvent {
        public Pre(Robot robot, ItemStack stack, World world, int x, int y, int z) {
            super(robot, stack, world, x, y, z);
        }
    }

    /**
     * Fired after a robot placed a block.
     */
    public static class Post extends RobotPlaceBlockEvent {
        public Post(Robot robot, ItemStack stack, World world, int x, int y, int z) {
            super(robot, stack, world, x, y, z);
        }
    }
}
