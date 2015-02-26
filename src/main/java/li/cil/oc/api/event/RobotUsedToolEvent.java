package li.cil.oc.api.event;

import li.cil.oc.api.internal.Agent;
import net.minecraft.item.ItemStack;

public class RobotUsedToolEvent extends RobotEvent {
    /**
     * The tool that was used, before and after use.
     */
    public final ItemStack toolBeforeUse, toolAfterUse;

    protected double damageRate;

    protected RobotUsedToolEvent(Agent agent, ItemStack toolBeforeUse, ItemStack toolAfterUse, double damageRate) {
        super(agent);
        this.toolBeforeUse = toolBeforeUse;
        this.toolAfterUse = toolAfterUse;
        this.damageRate = damageRate;
    }

    /**
     * The rate at which the used tool should lose durability, where one means
     * it loses durability at full speed, zero means it doesn't lose durability
     * at all.
     * <p/>
     * This value is in an interval of [0, 1].
     */
    public double getDamageRate() {
        return damageRate;
    }

    /**
     * Fired when a robot used a tool and is about to apply the damage rate to
     * partially undo the durability loss. This step is used to compute the
     * rate at which the tool should lose durability, which is used by the
     * experience upgrade, for example.
     */
    public static class ComputeDamageRate extends RobotUsedToolEvent {
        public ComputeDamageRate(Agent agent, ItemStack toolBeforeUse, ItemStack toolAfterUse, double damageRate) {
            super(agent, toolBeforeUse, toolAfterUse, damageRate);
        }

        /**
         * Set the rate at which the tool actually gets damaged.
         * <p/>
         * This will be clamped to an iterval of [0, 1].
         *
         * @param damageRate the new damage rate.
         */
        public void setDamageRate(double damageRate) {
            this.damageRate = Math.max(0, Math.min(1, damageRate));
        }
    }

    /**
     * Fired when a robot used a tool and the previously fired damage rate
     * computation returned a value smaller than one. The callbacks of this
     * method are responsible for applying the inverse damage the tool took.
     * The <tt>toolAfterUse</tt> item stack represents the actual tool, any
     * changes must be applied to that variable. The <tt>toolBeforeUse</tt>
     * item stack is passed for reference, to compute the actual amount of
     * durability that was lost. This may be required for tools where the
     * durability is stored in the item's NBT tag.
     */
    public static class ApplyDamageRate extends RobotUsedToolEvent {
        public ApplyDamageRate(Agent agent, ItemStack toolBeforeUse, ItemStack toolAfterUse, double damageRate) {
            super(agent, toolBeforeUse, toolAfterUse, damageRate);
        }
    }
}
