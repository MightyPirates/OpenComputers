package li.cil.oc.api.machine;

import li.cil.oc.api.Rotatable;
import net.minecraft.entity.player.EntityPlayer;

/**
 * This interface allows interaction with robots.
 * <p/>
 * It is intended to be used by components when installed in a robot. In that
 * case, the robot in question is the tile entity passed to item driver when
 * asked to create the component's environment.
 */
public interface Robot extends Rotatable {
    /**
     * Returns the fake player used to represent the robot as an entity for
     * certain actions that require one.
     * <p/>
     * This will automatically be positioned and rotated to represent the
     * robot's current position and rotation in the world. Use this to trigger
     * events involving the robot that require a player entity, and for
     * interacting with the robots' inventory.
     * <p/>
     * Note that the inventory of each robot is structured such that the first
     * four slots are the "equipment" slots, from left to right, i.e. slot one
     * is the tool slot, slot two is the card slot, three the disk slot and
     * slot four is for upgrades. The inventory proper starts after that.
     *
     * @return the fake player for the robot.
     */
    EntityPlayer player();

    /**
     * Gets the index of the currently selected slot in the robot's inventory.
     *
     * @return the index of the currently selected slot.
     */
    int selectedSlot();

    /**
     * Causes the currently installed upgrade to be saved and synchronized.
     * <p/>
     * If no upgrade is installed in the robot this does nothing.
     * <p/>
     * This is intended for upgrade components, to allow them to update their
     * client side representation for rendering purposes. The component will be
     * saved to its item's NBT tag compound, as it would be when the game is
     * saved, and then re-sent to the client. Keep the number of calls to this
     * function low, since each call causes a network packet to be sent.
     * <p/>
     * This is somewhat of a 'meh, it works' approach that I'm not really happy
     * with and plan to replace with something cleaner. Don't use unless you
     * absolutely really have to.
     */
    @Deprecated
    void saveUpgrade();
}
