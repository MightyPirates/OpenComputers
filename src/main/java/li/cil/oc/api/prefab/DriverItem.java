package li.cil.oc.api.prefab;

import li.cil.oc.api.Driver;
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
 * Note that if you use this prefab you <em>must instantiate your driver in the
 * init phase</em>, since it automatically registers itself with OpenComputers.
 * <p/>
 * You still have to specify your component's slot type and provide the
 * implementation for creating its environment, if any.
 *
 * @see li.cil.oc.api.network.ManagedEnvironment
 */
@SuppressWarnings("UnusedDeclaration")
public abstract class DriverItem implements li.cil.oc.api.driver.Item {
    protected final ItemStack[] items;

    protected DriverItem(final ItemStack... items) {
        this.items = items.clone();

        // Make the driver known with OpenComputers. This is required, otherwise
        // the mod won't know this driver exists. It must be called in the init
        // phase.
        Driver.add(this);
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
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound("tag"));
        }
        final NBTTagCompound nbt = stack.getTagCompound();
        // This is the suggested key under which to store item component data.
        // You are free to change this as you please.
        if (!nbt.hasKey("oc:data")) {
            nbt.setCompoundTag("oc:data", new NBTTagCompound());
        }
        return nbt.getCompoundTag("oc:data");
    }
}
