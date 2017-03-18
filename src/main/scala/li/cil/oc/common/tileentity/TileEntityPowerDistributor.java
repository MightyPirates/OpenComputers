package li.cil.oc.common.tileentity;

import li.cil.oc.Settings;
import li.cil.oc.api.Network;
import li.cil.oc.api.network.EnergyNode;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.NodeContainer;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.network.AbstractTileEntityNodeContainer;
import li.cil.oc.common.tileentity.traits.LocationTileEntityProxy;
import li.cil.oc.common.tileentity.traits.NotAnalyzable;
import li.cil.oc.common.tileentity.traits.PowerBridge;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;

import java.util.ArrayList;
import java.util.List;

public final class TileEntityPowerDistributor extends AbstractTileEntityMultiNodeContainer implements ITickable, LocationTileEntityProxy, NotAnalyzable, PowerBridge.PowerBalancerHost {
    // ----------------------------------------------------------------------- //
    // Persisted data.

    private final NodeContainer[] nodeContainers = new NodeContainer[EnumFacing.VALUES.length];

    // ----------------------------------------------------------------------- //
    // Computed data.

    private final List<EnergyNode> connectors = new ArrayList<>();
    private final PowerBridge balancer = new PowerBridge(this);

    // ----------------------------------------------------------------------- //

    public TileEntityPowerDistributor() {
        for (int i = 0; i < nodeContainers.length; i++) {
            nodeContainers[i] = new NodeContainerPowerDistributor(this);
            connectors.add((EnergyNode) nodeContainers[i].getNode());
        }
    }

    // ----------------------------------------------------------------------- //
    // AbstractTileEntityMultiNodeContainer

    @Override
    protected NodeContainer[] getEnvironments() {
        return nodeContainers;
    }

    // ----------------------------------------------------------------------- //
    // ITickable

    @Override
    public void update() {
        balancer.balance();
    }

    // ----------------------------------------------------------------------- //
    // PowerBalancerHost

    @Override
    public Iterable<EnergyNode> getConnectorsToBalance() {
        return connectors;
    }

    // ----------------------------------------------------------------------- //

    private static final class NodeContainerPowerDistributor extends AbstractTileEntityNodeContainer {
        NodeContainerPowerDistributor(final TileEntity host) {
            super(host);
        }

        @Override
        protected Node createNode() {
            return Network.newNode(this, Visibility.NONE).withConnector(Settings.get().bufferDistributor).create();
        }
    }
}
