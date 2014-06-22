package li.cil.oc.api.machine;

import li.cil.oc.api.Rotatable;
import li.cil.oc.api.network.Environment;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ISidedInventory;
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
 * <li>Tool</li>
 * <li><tt>containerCount</tt> hot-swappable components.</li>
 * <li><tt>inventorySize</tt> internal inventory slots.</li>
 * <li><tt>componentCount</tt> hard-wired components.</li>
 * </ul>
 * Note that there may be no hot-swappable (or even built-in) components or
 * no inventory, depending on the configuration of the robot. The hard-wired
 * components cannot be changed (removed/replaced).
 */
public interface Robot extends ISidedInventory, Rotatable {
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
     * <p/>
     * <em>Note</em>: this will always be three, regardless of the number of
     * installed containers. For unused slots the inventory will simply be
     * empty at that slot.
     */
    int containerCount();

    /**
     * The number of built-in components in this robot.
     */
    int componentCount();

    /**
     * The size of the internal inventory in this robot, excluding tool and
     * component slots.
     */
    int inventorySize();

    /**
     * Get the item stack in the specified inventory slot.
     * <p/>
     * This operates on the underlying, real inventory, as described in the
     * comment on top of this class.
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
     * <p/>
     * This is the index in the underlying, <em>real</em> inventory. To get
     * the 'local' index, i.e. the way the robot itself addresses it, add
     * one for the tool and <tt>containerCount</tt> to this value.
     *
     * @return the index of the currently selected slot.
     */
    int selectedSlot();

    /**
     * Sends the state of the <em>item</em> in the specified slot to the client
     * if it is an upgrade.
     * <p/>
     * Use this to update the state of an upgrade in that slot for rendering
     * purposes (e.g. this is used by the generator upgrade to update the
     * active state so the renderer knows which texture to use).
     * <p/>
     * This is necessary because inventories are not synchronized by default,
     * only if a player is currently 'looking into' the inventory (opened the
     * GUI of the inventory).
     * <p/>
     * The component will be saved to its item's NBT tag compound, as it would
     * be when the game is saved, and then the item is re-sent to the client.
     * Keep the number of calls to this function low, since each call causes a
     * network packet to be sent.
     */
    void synchronizeSlot(int slot);
}
