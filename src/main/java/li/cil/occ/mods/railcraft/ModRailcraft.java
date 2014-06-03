package li.cil.occ.mods.railcraft;

import li.cil.oc.api.Driver;
import li.cil.occ.mods.IMod;

public final class ModRailcraft implements IMod {
    @Override
    public String getModId() {
        return "Railcraft";
    }

    @Override
    public void initialize() {
    	Driver.add(new DriverBoilerFirebox());
    	Driver.add(new DriverSteamTurbine());   
    }
}
