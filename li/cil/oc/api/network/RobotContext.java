package li.cil.oc.api.network;

import net.minecraft.entity.player.EntityPlayer;

public interface RobotContext extends Context {
    /**
     * Gets the index of the currently selected slot in the robot's inventory.
     *
     * @return the index of the currently selected slot.
     */
    int selectedSlot();

    /**
     * Returns the fake player used to represent the robot as an entity for
     * certain actions that require one.
     * <p/>
     * This will automatically be positioned and rotated to represent the
     * robot's current position and rotation in the world. Use this to trigger
     * events involving the robot that require a player entity, and for
     * interacting with the robots' inventory.
     *
     * @return the fake player for the robot.
     */
    EntityPlayer player();
}
