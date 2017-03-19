package li.cil.oc.common.inventory;

import li.cil.oc.OpenComputers;
import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.NodeContainer;
import li.cil.oc.api.network.NodeContainerHost;
import li.cil.oc.api.network.NodeContainerItem;
import li.cil.oc.api.util.Location;
import li.cil.oc.common.Sound;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ComponentManager implements ICapabilityProvider, INBTSerializable<NBTTagList> {
    public interface ComponentInventoryHost extends Location {
        Node getItemNode();

        IItemHandler getComponentItems();
    }

    // ----------------------------------------------------------------------- //
    // Persisted data.

    private final NodeContainerItem[] environments;
    private final NodeContainerHost nodeContainerHost;

    // ----------------------------------------------------------------------- //
    // Computed data.

    private final ComponentInventoryHost host;
    private final List<ITickable> updatingEnvironments = new ArrayList<>();

    // ----------------------------------------------------------------------- //

    public ComponentManager(final ComponentInventoryHost host, final NodeContainerHost nodeContainerHost) {
        this.host = host;
        this.nodeContainerHost = nodeContainerHost;
        environments = new NodeContainerItem[host.getComponentItems().getSlots()];
    }

    @Nullable
    public NodeContainer getEnvironment(final int slot) {
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

            final DriverItem driver = Driver.driverFor(stack, nodeContainerHost.getClass());
            if (driver == null) {
                continue;
            }

            final NodeContainerItem environment = driver.createEnvironment(stack, nodeContainerHost);
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
            final NodeContainerItem environment = environments[slot];
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
        for (final NodeContainerItem environment : environments) {
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

    // ----------------------------------------------------------------------- //
    // ItemHandlerHost

    public void initializeComponent(final int slot, final ItemStack stack) {
        assert environments[slot] == null : "adding item without having removed previous one";
        disposeComponent(slot, ItemStack.EMPTY.copy()); // Handle assert not holding as gracefully as possible.

        final DriverItem driver = Driver.driverFor(stack, nodeContainerHost.getClass());
        if (driver == null) {
            return;
        }

        final NodeContainerItem environment = driver.createEnvironment(stack, nodeContainerHost);
        if (environment == null) {
            return;
        }

        environments[slot] = environment;

        if (environment instanceof ITickable) {
            updatingEnvironments.add((ITickable) environment);
        }

        environment.onInstalled(stack);

        if (Objects.equals(driver.slot(stack), Slot.Floppy)) {
            Sound.playDiskInsert(host);
        }
    }

    public void disposeComponent(final int slot, final ItemStack stack) {
        final NodeContainerItem environment = environments[slot];
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

        final DriverItem driver = Driver.driverFor(stack, nodeContainerHost.getClass());
        if (driver != null && Objects.equals(driver.slot(stack), Slot.Floppy)) {
            Sound.playDiskEject(host);
        }
    }

    // ----------------------------------------------------------------------- //
    // ICapabilityProvider

    @Override
    public boolean hasCapability(@Nonnull final Capability<?> capability, @Nullable final EnumFacing facing) {
        for (final NodeContainerItem environment : environments) {
            if (environment instanceof ICapabilityProvider) {
                final ICapabilityProvider provider = (ICapabilityProvider) environment;
                if (provider.hasCapability(capability, facing)) {
                    return true;
                }
            }
        }

        return false;
    }

    @Nullable
    @Override
    public <T> T getCapability(@Nonnull final Capability<T> capability, @Nullable final EnumFacing facing) {
        for (final NodeContainerItem environment : environments) {
            if (environment instanceof ICapabilityProvider) {
                final ICapabilityProvider provider = (ICapabilityProvider) environment;
                final T instance = provider.getCapability(capability, facing);
                if (instance != null) {
                    return instance;
                }
            }
        }
        return null;
    }

    // ----------------------------------------------------------------------- //
    // INBTSerializable

    @Override
    public NBTTagList serializeNBT() {
        final NBTTagList nbt = new NBTTagList();
        for (final NodeContainerItem environment : environments) {
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
