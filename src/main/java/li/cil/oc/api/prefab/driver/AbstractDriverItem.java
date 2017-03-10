package li.cil.oc.api.prefab.driver;

import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.EnvironmentItem;
import li.cil.oc.api.tileentity.Rotatable;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * If you wish to create item components such as the network card or hard drives
 * you will need an item driver.
 * <p/>
 * This prefab allows creating a driver that works for a specified list of item
 * stacks (to support different items with the same id but different damage
 * values). It also takes care of creating and getting the tag compound on an
 * item stack to save data to or load data from.
 * <p/>
 * You still have to specify your component's slot type and provide the
 * implementation for creating its environment, if any.
 *
 * @see EnvironmentItem
 */
@SuppressWarnings("UnusedDeclaration")
public abstract class AbstractDriverItem implements DriverItem {
    // This is the suggested key under which to store item component data.
    // You are free to change this as you please.
    private static final String TAG_DATA = "oc:data";

    protected final ItemStack[] items;

    protected AbstractDriverItem(final ItemStack... items) {
        this.items = items.clone();
    }

    @Override
    public boolean worksWith(final ItemStack stack) {
        if (stack != null) {
            for (ItemStack item : items) {
                if (item != null && item.isItemEqual(stack)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public int tier(final ItemStack stack) {
        return 0;
    }

    @Override
    public NBTTagCompound dataTag(final ItemStack stack) {
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null) {
            stack.setTagCompound(nbt = new NBTTagCompound());
        }
        if (!nbt.hasKey(TAG_DATA)) {
            nbt.setTag(TAG_DATA, new NBTTagCompound());
        }
        return nbt.getCompoundTag(TAG_DATA);
    }

    // Convenience methods provided for HostAware drivers.

    protected boolean isAdapter(Class<? extends EnvironmentHost> host) {
        return li.cil.oc.api.internal.Adapter.class.isAssignableFrom(host);
    }

    protected boolean isComputer(Class<? extends EnvironmentHost> host) {
        return li.cil.oc.api.internal.Case.class.isAssignableFrom(host);
    }

    protected boolean isRobot(Class<? extends EnvironmentHost> host) {
        return li.cil.oc.api.internal.Robot.class.isAssignableFrom(host);
    }

    protected boolean isRotatable(Class<? extends EnvironmentHost> host) {
        return Rotatable.class.isAssignableFrom(host);
    }

    protected boolean isServer(Class<? extends EnvironmentHost> host) {
        return li.cil.oc.api.internal.Server.class.isAssignableFrom(host);
    }

    protected boolean isTablet(Class<? extends EnvironmentHost> host) {
        return li.cil.oc.api.internal.Tablet.class.isAssignableFrom(host);
    }
}
