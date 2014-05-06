package li.cil.oc.api.machine;

import li.cil.oc.api.Rotatable;
import li.cil.oc.api.network.Environment;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

/**
 * This interface allows interaction with robots.
 * <p/>
 * It is intended to be used by components when installed in a robot. In that
 * case, the robot in question is the tile entity passed to item driver when
 * asked to create the component's environment.
 * <p/>
 * A robot's inventory contains component items and items in the actual
 * inventory. The physical layout in the underlying 'real' inventory is as
 * follows:
 * <ul>
 * <li>Slot 0: Tool</li>
 * <li>Slot [1, dynamicComponentCapacity + 1): hot-swappable components.</li>
 * <li>Slot [dynamicComponentCapacity + 1, componentCapacity + 1): hard-wired components.</li>
 * <li>Slot [componentCapacity + 1, inventorySize): actual inventory.</li>
 * </ul>
 * Note that either of these intervals may be empty, depending on the parts
 * the robot is built from.
 */
public interface Robot extends Rotatable {
    /**
     * Returns the fake player used to represent the robot as an entity for
     * certain actions that require one.
     * <p/>
     * This will automatically be positioned and rotated to represent the
     * robot's current position and rotation in the world. Use this to trigger
     * events involving the robot that require a player entity, and for more
     * in-depth interaction with the robots' inventory.
     *
     * @return the fake player for the robot.
     */
    EntityPlayer player();

    /**
     * The number of hot-swappable component slots in this robot.
     */
    int dynamicComponentCapacity();

    /**
     * The <em>total</em> number of component slots in this robot, including
     * hot-swappable component slots.
     */
    int componentCapacity();

    /**
     * The <em>total</em> inventory space in this robot, including tool and
     * component slots.
     */
    int inventorySize();

    /**
     * Get the item stack in the specified inventory slot.
     * <p/>
     * This operates on the underlying, real inventory, as described in the
     * comment on top of this class. The starting index of the part of the
     * inventory that is accessible to the robot for manipulation is at
     * <tt>componentCapacity + 1</tt>.
     * <p/>
     * This will return <tt>null</tt> for empty slots.
     *
     * @param index the index of the slot from which to get the stack.
     * @return the content of that slot, or <tt>null</tt>.
     */
    ItemStack getStackInSlot(int index);

    /**
     * Get the environment for the component in the specified slot.
     * <p/>
     * This operates on the underlying, real inventory, as described in the
     * comment on top of this class.
     * <p/>
     * This will return <tt>null</tt> for slots that do not contain components,
     * or components that do not have an environment (on the calling side).
     *
     * @param index the index of the slot from which to get the environment.
     * @return the environment for that slot, or <tt>null</tt>.
     */
    Environment getComponentInSlot(int index);

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
