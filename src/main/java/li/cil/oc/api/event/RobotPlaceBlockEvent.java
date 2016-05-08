package li.cil.oc.api.event;

import li.cil.oc.api.internal.Agent;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.Cancelable;

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

    protected RobotPlaceBlockEvent(Agent agent, ItemStack stack, World world, BlockPos pos) {
        super(agent);
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
        public Pre(Agent agent, ItemStack stack, World world, BlockPos pos) {
            super(agent, stack, world, pos);
        }
    }

    /**
     * Fired after a robot placed a block.
     */
    public static class Post extends RobotPlaceBlockEvent {
        public Post(Agent agent, ItemStack stack, World world, BlockPos pos) {
            super(agent, stack, world, pos);
        }
    }
}
