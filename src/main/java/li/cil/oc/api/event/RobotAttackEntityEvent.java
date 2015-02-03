package li.cil.oc.api.event;

import cpw.mods.fml.common.eventhandler.Cancelable;
import li.cil.oc.api.internal.Agent;
import net.minecraft.entity.Entity;

public class RobotAttackEntityEvent extends RobotEvent {
    /**
     * The entity that the robot will attack.
     */
    public final Entity target;

    protected RobotAttackEntityEvent(Agent agent, Entity target) {
        super(agent);
        this.target = target;
    }

    /**
     * Fired when a robot is about to attack an entity.
     * <p/>
     * Canceling this event will prevent the attack.
     */
    @Cancelable
    public static class Pre extends RobotAttackEntityEvent {
        public Pre(Agent agent, Entity target) {
            super(agent, target);
        }
    }

    /**
     * Fired after a robot has attacked an entity.
     */
    public static class Post extends RobotAttackEntityEvent {
        public Post(Agent agent, Entity target) {
            super(agent, target);
        }
    }
}
