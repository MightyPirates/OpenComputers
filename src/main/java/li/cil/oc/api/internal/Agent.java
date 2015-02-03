package li.cil.oc.api.internal;

import li.cil.oc.api.machine.MachineHost;
import net.minecraft.entity.player.EntityPlayer;

/**
 * General marker interface for autonomous agents such as robots and drones.
 */
public interface Agent extends MachineHost {
    /**
     * Returns the fake player used to represent the agent as an entity for
     * certain actions that require one.
     * <p/>
     * This will automatically be positioned and rotated to represent the
     * agent's current position and rotation in the world. Use this to trigger
     * events involving the agent that require a player entity.
     * <p/>
     * Note that this <em>may</em> be the common OpenComputers fake player.
     *
     * @return the fake player for the agent.
     */
    EntityPlayer player();
}
