package li.cil.oc.common.capabilities;

import li.cil.oc.api.network.NodeContainer;
import li.cil.oc.api.tileentity.Colored;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;

// Gotta be Java, @CapabilityInject don't werk for Scala ;_;
public final class Capabilities {
    @CapabilityInject(Colored.class)
    public static Capability<Colored> ColoredCapability;

    @CapabilityInject(NodeContainer.class)
    public static Capability<NodeContainer> EnvironmentCapability;

    @CapabilityInject(SidedEnvironment.class)
    public static Capability<SidedEnvironment> SidedEnvironmentCapability;

    public static void init() {
        CapabilityManager.INSTANCE.register(NodeContainer.class, new CapabilityEnvironment.DefaultStorage(), CapabilityEnvironment.DefaultImpl.class);
        CapabilityManager.INSTANCE.register(SidedEnvironment.class, new CapabilitySidedEnvironment.DefaultStorage(), CapabilitySidedEnvironment.DefaultImpl.class);
        CapabilityManager.INSTANCE.register(Colored.class, new CapabilityColored.DefaultStorage(), CapabilityColored.DefaultImpl.class);
    }

    private Capabilities() {
    }
}
