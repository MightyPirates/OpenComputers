package li.cil.oc.common.tileentity;

import li.cil.oc.api.network.NodeContainer;
import li.cil.oc.common.tileentity.traits.NodeContainerHostTileEntity;
import li.cil.oc.server.component.Geolyzer;

public final class TileEntityGeolyzer extends AbstractTileEntitySingleNodeContainer {
    // ----------------------------------------------------------------------- //
    // Persisted data.

    private final NodeContainer geolyzer = new Geolyzer(new NodeContainerHostTileEntity(this));

    // ----------------------------------------------------------------------- //
    // AbstractTileEntitySingleNodeContainer

    @Override
    protected NodeContainer getNodeContainer() {
        return geolyzer;
    }
}
