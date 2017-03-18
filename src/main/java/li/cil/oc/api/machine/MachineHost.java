package li.cil.oc.api.machine;

import li.cil.oc.api.network.NodeContainerHost;
import net.minecraft.item.ItemStack;

/**
 * This interface has to be implemented by 'hosts' of machine instances.
 * <p/>
 * It provides some context for the machine, in particular which world it is
 * running in, to allow querying the time of day, for example.
 */
public interface MachineHost extends NodeContainerHost {
    /**
     * The machine currently hosted.
     */
    Machine getMachine();

    /**
     * List of all components that are built into this machine directly.
     * <p/>
     * This is used to find CPUs, component buses and memory.
     */
    Iterable<ItemStack> internalComponents();
}
