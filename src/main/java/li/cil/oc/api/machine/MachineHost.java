package li.cil.oc.api.machine;

import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Node;
import net.minecraft.world.World;

/**
 * This interface has to be implemented by 'hosts' of machine instances.
 * <p/>
 * It provides some context for the machine, in particular which world it is
 * running in, to allow querying the time of day, for example.
 */
public interface MachineHost extends Context {
    /**
     * The X coordinate of this machine owner in the world, in block coordinates.
     */
    int x();

    /**
     * The Y coordinate of this machine owner in the world, in block coordinates.
     */
    int y();

    /**
     * The Z coordinate of this machine owner in the world, in block coordinates.
     */
    int z();

    /**
     * The world the machine is running in, e.g. if the owner is a tile entity
     * this is the world the tile entity lives in.
     *
     * @return the world the machine runs in.
     */
    World world();

    /**
     * Get the architecture to use in the hosted machine.
     * <p/>
     * This can be a static architecture type, but will usually be based on the
     * CPU installed in the host (for example, this is true for computer cases,
     * servers, robots and tablets).
     *
     * @return the architecture of the installed CPU, or <tt>null</tt>.
     */
    Class<? extends Architecture> cpuArchitecture();

    /**
     * The amount of memory (RAM) made available to the machine, in bytes.
     * <p/>
     * This is usually determined by the components installed in the owner, for
     * example the memory sticks in a computer case.
     *
     * @return the amount of memory that can be used by the machine, in bytes.
     */
    int installedMemory();

    /**
     * The number of components the machine can address without crashing.
     * <p/>
     * This is usually determined by the components installed in the owner, for
     * example the CPUs in a server.
     * <p/>
     * Note that the component count does <em>not</em> include file systems.
     *
     * @return the number of supported components.
     */
    int maxComponents();

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
     * This is called by the machine when its state changed (which can be
     * multiple times per actual game tick), to notify the owner that it should
     * save its state on the next world save.
     * <p/>
     * This method is called from executor threads, so it must be thread-safe.
     */
    void markAsChanged();

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
