package li.cil.oc.common.tileentity;

import li.cil.oc.api.network.NodeContainer;
import li.cil.oc.server.component.Transposer;

public final class TileEntityTransposer extends AbstractTileEntitySingleNodeContainer {
    // ----------------------------------------------------------------------- //
    // Persisted data.

    private final NodeContainer nodeContainer = new Transposer.Block(this);

    // ----------------------------------------------------------------------- //
    // Computed data.

    // Used on client side to check whether to render activity indicators.
    public long lastOperation = 0L;

    // ----------------------------------------------------------------------- //
    // AbstractTileEntitySingleNodeContainer

    @Override
    protected NodeContainer getNodeContainer() {
        return nodeContainer;
    }
}
