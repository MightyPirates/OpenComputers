package li.cil.oc.integration.railcraft;

import li.cil.oc.api.Driver;
import li.cil.oc.integration.IMod;
import li.cil.oc.util.mods.Mods;

public final class ModRailcraft implements IMod {
    @Override
    public Mods.Mod getMod() {
        return Mods.Railcraft();
    }

    @Override
    public void initialize() {
        Driver.add(new DriverBoilerFirebox());
        Driver.add(new DriverSteamTurbine());
    }
}
