package li.cil.oc.api.machine;

import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Node;
import net.minecraft.item.ItemStack;

/**
 * This interface has to be implemented by 'hosts' of machine instances.
 * <p/>
 * It provides some context for the machine, in particular which world it is
 * running in, to allow querying the time of day, for example.
 */
public interface MachineHost extends EnvironmentHost {
    /**
     * The machine currently hosted.
     */
    Machine machine();

    /**
     * List of all components that are built into this machine directly.
     * <p/>
     * This is used to find CPUs, component buses and memory.
     */
    Iterable<ItemStack> internalComponents();

    /**
     * Get the slot a component with the specified address is in.
     * <p/>
     * This is intended to allow determining the slot of <em>item</em>
     * components sitting in computers. For other components this returns
     * negative values.
     *
     * @param address the address of the component to get the slot for.
     * @return the index of the slot the component is in.
     */
    int componentSlot(String address);

    /**
     * This is called on the owner when the machine's {@link Environment#onConnect(Node)}
     * method gets called. This can be useful for reacting to network events
     * when the owner does not have its own node (for example, computer cases
     * expose their machine's node as their own node). This callback allows it
     * to connect its components (graphics cards and the like) when it is
     * connected to a node network (when added to the world, for example).
     *
     * @param node the node that was connected to the network.
     */
    void onMachineConnect(Node node);

    /**
     * Like {@link #onMachineConnect(Node)}, except that this is called whenever
     * the machine's {@link Environment#onDisconnect(Node)} method is called.
     *
     * @param node the node that was disconnected from the network.
     */
    void onMachineDisconnect(Node node);
}
