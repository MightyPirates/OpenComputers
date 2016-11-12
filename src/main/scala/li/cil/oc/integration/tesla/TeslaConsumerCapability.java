package li.cil.oc.integration.tesla;

import net.darkhax.tesla.api.ITeslaConsumer;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;

public enum TeslaConsumerCapability {
    INSTANCE;

    @CapabilityInject(ITeslaConsumer.class)
    public static Capability<ITeslaConsumer> CONSUMER_CAPABILITY = null;
}
