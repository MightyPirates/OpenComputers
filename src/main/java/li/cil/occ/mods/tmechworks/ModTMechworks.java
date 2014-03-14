package li.cil.occ.mods.tmechworks;

import li.cil.oc.api.Driver;
import li.cil.occ.mods.IMod;

public final class ModTMechworks implements IMod {
    @Override
    public String getModId() {
        return "TMechworks";
    }

    @Override
    public void initialize() {
        Driver.add(new DriverDrawBridge());
    }
}
