package li.cil.oc.common.tileentity;

import li.cil.oc.Settings;
import li.cil.oc.api.Network;
import li.cil.oc.api.network.*;
import li.cil.oc.api.prefab.network.AbstractEnvironment;
import li.cil.oc.common.tileentity.traits.NotAnalyzable;
import li.cil.oc.common.tileentity.traits.PowerBalancer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;

import java.util.ArrayList;
import java.util.List;

public final class TileEntityPowerDistributor extends AbstractTileEntityMultiEnvironment implements NotAnalyzable, PowerBalancer.PowerBalancerHost, ITickable {
    // ----------------------------------------------------------------------- //
    // Persisted data.

    private final Environment[] environments = new Environment[EnumFacing.VALUES.length];

    // ----------------------------------------------------------------------- //
    // Computed data.

    private final List<Connector> connectors = new ArrayList<>();
    private final PowerBalancer balancer = new PowerBalancer(this);

    // ----------------------------------------------------------------------- //

    public TileEntityPowerDistributor() {
        for (int i = 0; i < environments.length; i++) {
            environments[i] = new EnvironmentPowerDistributor(this);
            connectors.add((Connector) environments[i].getNode());
        }
    }

    // ----------------------------------------------------------------------- //
    // AbstractTileEntityMultiEnvironment

    @Override
    protected Environment[] getEnvironments() {
        return environments;
    }

    // ----------------------------------------------------------------------- //
    // PowerBalancerHost

    @Override
    public Iterable<Connector> getConnectorsToBalance() {
        return connectors;
    }

    // ----------------------------------------------------------------------- //
    // ITickable

    @Override
    public void update() {
        balancer.balance();
    }

    // ----------------------------------------------------------------------- //

    private static final class EnvironmentPowerDistributor extends AbstractEnvironment {
        EnvironmentPowerDistributor(final EnvironmentHost host) {
            super(host);
        }

        @Override
        protected Node createNode() {
            return Network.newNode(this, Visibility.NONE).withConnector(Settings.get().bufferDistributor()).create();
        }
    }
}
