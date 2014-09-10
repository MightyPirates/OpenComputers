package li.cil.occ.mods.gregtech;

import li.cil.oc.api.Driver;
import li.cil.occ.mods.IMod;

public final class ModGregtech implements IMod {
    public static final String MOD_ID = "gregtech";

    @Override
    public String getModId() {
        return MOD_ID;
    }

    @Override
    public void initialize() {
        Driver.add(new DriverEnergyContainer());
    }
}
