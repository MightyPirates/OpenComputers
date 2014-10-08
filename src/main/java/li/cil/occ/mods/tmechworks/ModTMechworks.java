package li.cil.occ.mods.tmechworks;

import li.cil.oc.api.Driver;
import li.cil.occ.mods.IMod;

public final class ModTMechworks implements IMod {
    public static final String MOD_ID = "TMechworks";

    @Override
    public String getModId() {
        return MOD_ID;
    }

    @Override
    public void initialize() {
        Driver.add(new DriverDrawBridge());
    }
}
