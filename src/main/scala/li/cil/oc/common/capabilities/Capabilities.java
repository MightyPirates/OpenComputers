package li.cil.oc.common.capabilities;

import li.cil.oc.api.internal.Colored;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.SidedEnvironment;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;

// Gotta be Java, @CapabilityInject don't werk for Scala ;_;
public final class Capabilities {
    @CapabilityInject(Colored.class)
    public static Capability<Colored> ColoredCapability;

    @CapabilityInject(Environment.class)
    public static Capability<Environment> EnvironmentCapability;

    @CapabilityInject(SidedEnvironment.class)
    public static Capability<SidedEnvironment> SidedEnvironmentCapability;

    public static void init() {
        CapabilityManager.INSTANCE.register(Environment.class, new CapabilityEnvironment.DefaultStorage(), CapabilityEnvironment.DefaultImpl::new);
        CapabilityManager.INSTANCE.register(SidedEnvironment.class, new CapabilitySidedEnvironment.DefaultStorage(), CapabilitySidedEnvironment.DefaultImpl::new);
        CapabilityManager.INSTANCE.register(Colored.class, new CapabilityColored.DefaultStorage(), CapabilityColored.DefaultImpl::new);
    }

    private Capabilities() {
    }
}
