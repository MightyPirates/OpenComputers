package li.cil.oc.common.capabilities;

import li.cil.oc.api.internal.Colored;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.SidedEnvironment;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;

import java.util.concurrent.Callable;

// Gotta be Java, @CapabilityInject don't werk for Scala ;_;
public final class Capabilities {
    @CapabilityInject(Colored.class)
    public static Capability<Colored> ColoredCapability;

    @CapabilityInject(Environment.class)
    public static Capability<Environment> EnvironmentCapability;

    @CapabilityInject(SidedEnvironment.class)
    public static Capability<SidedEnvironment> SidedEnvironmentCapability;

    // java 7 doesn't have generic type constraints
    // java 7 doesn't have lambdas
    // java 7 doesn't generic type covariance
    private static class StupidJavaTookTooManyYearsToIntroduceLambdas<T> implements Callable<T> {

        StupidJavaTookTooManyYearsToIntroduceLambdas(Class cls) {
            _cls = cls;
        }

        @Override
        public T call() throws Exception {
            return (T)_cls.newInstance();
        }

        private Class _cls;
    }

    public static void init() {
        CapabilityManager.INSTANCE.register(Environment.class, new CapabilityEnvironment.DefaultStorage(), new StupidJavaTookTooManyYearsToIntroduceLambdas<Environment>(CapabilityEnvironment.DefaultImpl.class));
        CapabilityManager.INSTANCE.register(SidedEnvironment.class, new CapabilitySidedEnvironment.DefaultStorage(), new StupidJavaTookTooManyYearsToIntroduceLambdas<SidedEnvironment>(CapabilitySidedEnvironment.DefaultImpl.class));
        CapabilityManager.INSTANCE.register(Colored.class, new CapabilityColored.DefaultStorage(), new StupidJavaTookTooManyYearsToIntroduceLambdas<Colored>(CapabilityColored.DefaultImpl.class));
    }

    private Capabilities() {
    }
}
