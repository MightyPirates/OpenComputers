package li.cil.oc.api.internal;

import li.cil.oc.api.machine.MachineHost;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;

import java.util.UUID;

/**
 * General marker interface for autonomous agents such as robots and drones.
 */
public interface Agent extends MachineHost, Rotatable {
    /**
     * The equipment inventory of this agent.
     * <p/>
     * For example, for the robot this is the tool slot as well as slots
     * provided by containers installed in the robot, if any.
     * <p/>
     * If an agent has no equipment slots this will be a zero-sized inventory.
     */
    IInventory equipmentInventory();

    /**
     * The main inventory of this agent, which it (usually) also can
     * interact with on its own.
     * <p/>
     * If an agent has no inventory slots this will be a zero-sized inventory.
     */
    IInventory mainInventory();

    /**
     * Provides access to the tanks of the agent.
     * <p/>
     * If an agent has no tanks this will be a zero-sized multi-tank.
     */
    MultiTank tank();

    /**
     * Gets the index of the currently selected slot in the agent's inventory.
     */
    int selectedSlot();

    /**
     * Set the index of the currently selected slot.
     */
    void setSelectedSlot(int index);

    /**
     * Get the index of the currently selected tank.
     */
    int selectedTank();

    /**
     * Set the index of the currently selected tank.
     */
    void setSelectedTank(int index);

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

    /**
     * Get the name of this agent.
     */
    String name();

    /**
     * Set the name of the agent.
     */
    void setName(String name);

    /**
     * The name of the player owning this agent, e.g. the player that placed it.
     */
    String ownerName();

    /**
     * The UUID of the player owning this agent, e.g. the player that placed it.
     */
    UUID ownerUUID();
}
