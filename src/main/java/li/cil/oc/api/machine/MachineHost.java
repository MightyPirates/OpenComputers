package li.cil.oc.api.machine;

import li.cil.oc.api.driver.EnvironmentHost;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Node;

/**
 * This interface has to be implemented by 'hosts' of machine instances.
 * <p/>
 * It provides some context for the machine, in particular which world it is
 * running in, to allow querying the time of day, for example.
 */
public interface MachineHost extends EnvironmentHost {
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
     * This determines how many direct calls the machine can make per tick.
     * <p/>
     * A call to a direct method with a limit will consume <tt>1 / limit</tt>
     * of the available call budget. When the budget reaches zero, the machine
     * is forced into a synchronized call to make it wait for the next tick.
     * <p/>
     * The default values used by OC are 0.5, 1.0 and 1.5 for a tier one, two
     * and three CPU, respectively.
     * <p/>
     * The call budget is reset to this value each tick.
     *
     * @return the direct call budget, per tick.
     */
    double callBudget();

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
    void markForSaving();

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
