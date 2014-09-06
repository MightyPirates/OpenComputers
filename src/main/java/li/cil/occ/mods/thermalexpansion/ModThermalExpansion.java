package li.cil.occ.mods.thermalexpansion;

import li.cil.oc.api.Driver;
import li.cil.occ.mods.IMod;

public final class ModThermalExpansion implements IMod {
    public static final String MOD_ID = "ThermalExpansion";

    @Override
    public String getModId() {
        return MOD_ID;
    }

    @Override
    public void initialize() {
        Driver.add(new DriverLamp());
    }
}
