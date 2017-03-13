package li.cil.oc.common.tileentity.traits;

import li.cil.oc.Settings;
import li.cil.oc.api.network.EnergyNode;
import li.cil.oc.api.util.Location;
import li.cil.oc.api.network.Network;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * Bridges power across multiple sub-networks by distributing it so that
 * each sub-network has the same relative amount of energy.
 */
public final class PowerBridge {
    public interface PowerBalancerHost extends Location {
        Iterable<EnergyNode> getConnectorsToBalance();
    }

    private final PowerBalancerHost host;

    public PowerBridge(final PowerBalancerHost host) {
        this.host = host;
    }

    public void balance() {
        final World world = host.getHostWorld();

        if (world.getTotalWorldTime() % Settings.get().tickFrequency() != 0) {
            return;
        }

        final Iterable<EnergyNode> connectors = host.getConnectorsToBalance();

        final Set<Network> networks = new HashSet<>();
        for (final EnergyNode connector : connectors) {
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
