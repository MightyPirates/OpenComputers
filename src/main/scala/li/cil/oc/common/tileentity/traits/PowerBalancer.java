package li.cil.oc.common.tileentity.traits;

import li.cil.oc.Settings;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Network;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

public final class PowerBalancer {
    public interface PowerBalancerHost extends EnvironmentHost {
        Iterable<Connector> getConnectorsToBalance();
    }

    private final PowerBalancerHost host;

    public PowerBalancer(final PowerBalancerHost host) {
        this.host = host;
    }

    public void balance() {
        final World world = host.getHostWorld();

        if (world.getTotalWorldTime() % Settings.get().tickFrequency() != 0) {
            return;
        }

        final Iterable<Connector> connectors = host.getConnectorsToBalance();

        final Set<Network> networks = new HashSet<>();
        for (final Connector connector : connectors) {
            final Network network = connector.getNetwork();
            if (network == null) {
                continue;
            }
            networks.add(network);
        }

        forEachLock(networks, ReentrantLock::lock);

        double sum = 0;
        double sizeSum = 0;
        for (final Network network : networks) {
            sum += network.getGlobalBuffer();
            sizeSum += network.getGlobalBufferSize();
        }

        final double fillRatio = MathHelper.clamp(sum / sizeSum, 0, 1);
        for (final Network network : networks) {
            network.changeBuffer(network.getGlobalBufferSize() * fillRatio - network.getGlobalBuffer());
        }

        forEachLock(networks, ReentrantLock::unlock);
    }

    private static void forEachLock(final Iterable<Network> networks, final Consumer<ReentrantLock> consumer) {
        for (final Network network : networks) {
            if (network != null) {
                if (network instanceof NetworkImpl) {
                    final NetworkImlp networkImpl = (NetworkImpl) network;
                    final ReentrantLock lock = networkImpl.getLock();
                    consumer.accept(lock);
                }
            }
        }
    }
}
