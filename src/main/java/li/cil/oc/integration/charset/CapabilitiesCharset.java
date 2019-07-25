package li.cil.oc.integration.charset;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import pl.asie.charset.api.wires.IBundledEmitter;
import pl.asie.charset.api.wires.IBundledReceiver;
import pl.asie.charset.api.wires.IRedstoneEmitter;
import pl.asie.charset.api.wires.IRedstoneReceiver;

public final class CapabilitiesCharset {
    @CapabilityInject(IBundledEmitter.class)
    public static Capability<IBundledEmitter> BUNDLED_EMITTER;
    @CapabilityInject(IBundledReceiver.class)
    public static Capability<IBundledReceiver> BUNDLED_RECEIVER;
    @CapabilityInject(IRedstoneEmitter.class)
    public static Capability<IRedstoneEmitter> REDSTONE_EMITTER;
    @CapabilityInject(IRedstoneReceiver.class)
    public static Capability<IRedstoneReceiver> REDSTONE_RECEIVER;
}