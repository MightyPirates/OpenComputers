package li.cil.oc.common.inventory;

import li.cil.oc.OpenComputers;
import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.EnvironmentItem;
import li.cil.oc.api.network.Node;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public final class ComponentInventory implements ItemHandlerHosted.ItemHandlerHost, INBTSerializable<NBTTagList> {
    public interface ComponentInventoryHost extends EnvironmentHost {
        Node getItemNode();

        IItemHandler getComponentItems();
    }

    // ----------------------------------------------------------------------- //

    private final ComponentInventoryHost host;
    private final EnvironmentItem[] environments;
    private final List<ITickable> updatingEnvironments = new ArrayList<>();

    // ----------------------------------------------------------------------- //

    public ComponentInventory(final ComponentInventoryHost host) {
        this.host = host;
        environments = new EnvironmentItem[host.getComponentItems().getSlots()];
    }

    @Nullable
    public Environment getEnvironment(final int slot) {
        return environments[slot];
    }

    public void update() {
        // No foreach to allow components to remove themselves in their update.
        for (int i = updatingEnvironments.size() - 1; i >= 0; i--) {
            if (i < updatingEnvironments.size()) { // Just in case an update triggered a multi-remove.
                updatingEnvironments.get(i).update();
            }
        }
    }

    public void initialize() {
        dispose();

        final IItemHandler inventory = host.getComponentItems();
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            final ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }

            final DriverItem driver = Driver.driverFor(stack, host.getClass());
            if (driver == null) {
                continue;
            }

            final EnvironmentItem environment = driver.createEnvironment(stack, host);
            if (environment == null) {
                continue;
            }

            environments[slot] = environment;

            if (environment instanceof ITickable) {
                updatingEnvironments.add((ITickable) environment);
            }
        }
    }

    public void dispose() {
        for (int slot = 0; slot < environments.length; slot++) {
            final EnvironmentItem environment = environments[slot];
            if (environment == null) {
                continue;
            }

            environments[slot] = null;

            if (environment instanceof ITickable) {
                updatingEnvironments.remove(environment);
            }

            final Node node = environment.getNode();
            if (node != null) {
                node.remove();
            }

            environment.onDispose();
        }
    }

    public void connect() {
        for (final EnvironmentItem environment : environments) {
            if (environment == null) {
                continue;
            }

            final Node node = environment.getNode();
            if (node == null) {
                continue;
            }

            host.getItemNode().connect(environment.getNode());
        }
    }

    @Override
    public void onItemAdded(final int slot, final ItemStack stack) {
        assert environments[slot] == null : "adding item without having removed previous one";
        onItemRemoved(slot, ItemStack.EMPTY.copy()); // Handle assert not holding as gracefully as possible.

        final DriverItem driver = Driver.driverFor(stack, host.getClass());
        if (driver == null) {
            return;
        }

        final EnvironmentItem environment = driver.createEnvironment(stack, host);
        if (environment == null) {
            return;
        }

        environments[slot] = environment;

        if (environment instanceof ITickable) {
            updatingEnvironments.add((ITickable) environment);
        }

        environment.onInstalled(stack);
    }

    @Override
    public void onItemRemoved(final int slot, final ItemStack stack) {
        final EnvironmentItem environment = environments[slot];
        if (environment != null) {
            if (environment instanceof ITickable) {
                updatingEnvironments.remove(environment);
            }

            environments[slot] = null;

            final Node node = environment.getNode();
            if (node != null) {
                node.remove();
            }

            environment.onUninstalled(stack);

            environment.onDispose();
        }
    }

    // ----------------------------------------------------------------------- //
    // INBTSerializable

    @Override
    public NBTTagList serializeNBT() {
        final NBTTagList nbt = new NBTTagList();
        for (final EnvironmentItem environment : environments) {
            if (environment != null) {
                nbt.appendTag(environment.serializeNBT());
            } else {
                nbt.appendTag(new NBTTagCompound());
            }
        }
        return nbt;
    }

    @Override
    public void deserializeNBT(final NBTTagList nbt) {
        initialize();

        if (nbt.tagCount() != environments.length) {
            OpenComputers.log().warn("component count mismatch; not loading component states.");
            return;
        }

        for (int slot = 0; slot < nbt.tagCount(); slot++) {
            if (environments[slot] != null) {
                environments[slot].deserializeNBT(nbt.getCompoundTagAt(slot));
            }
        }
    }
}
