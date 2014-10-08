package li.cil.occ.mods.railcraft;

import li.cil.oc.api.Driver;
import li.cil.occ.mods.IMod;

public final class ModRailcraft implements IMod {
    public static final String MOD_ID = "Railcraft";

    @Override
    public String getModId() {
        return MOD_ID;
    }

    @Override
    public void initialize() {
        Driver.add(new DriverBoilerFirebox());
        Driver.add(new DriverSteamTurbine());
    }
}
