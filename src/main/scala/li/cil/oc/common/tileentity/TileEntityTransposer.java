package li.cil.oc.common.tileentity;

import li.cil.oc.api.network.Environment;
import li.cil.oc.server.component.Transposer;

public final class TileEntityTransposer extends AbstractTileEntityEnvironmentHost {
    // ----------------------------------------------------------------------- //
    // Persisted data.

    private final Environment environment = new Transposer.Block(this);

    // ----------------------------------------------------------------------- //
    // Computed data.

    // Used on client side to check whether to render activity indicators.
    public long lastOperation = 0L;

    // ----------------------------------------------------------------------- //
    // AbstractTileEntityEnvironmentHost

    @Override
    protected Environment getEnvironment() {
        return environment;
    }
}
