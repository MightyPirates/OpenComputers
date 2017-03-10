package li.cil.oc.common.tileentity;

import li.cil.oc.api.network.Environment;
import li.cil.oc.server.component.Geolyzer;

public final class TileEntityGeolyzer extends AbstractTileEntitySingleEnvironment {
    // ----------------------------------------------------------------------- //
    // Persisted data.

    private final Environment geolyzer = new Geolyzer(this);

    // ----------------------------------------------------------------------- //
    // AbstractTileEntityEnvironmentHost

    @Override
    protected Environment getEnvironment() {
        return geolyzer;
    }
}
