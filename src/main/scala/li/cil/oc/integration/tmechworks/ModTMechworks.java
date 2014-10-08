package li.cil.oc.integration.tmechworks;

import li.cil.oc.api.Driver;
import li.cil.oc.integration.IMod;
import li.cil.oc.util.mods.Mods;

public final class ModTMechworks implements IMod {
    @Override
    public Mods.Mod getMod() {
        return Mods.TMechWorks();
    }

    @Override
    public void initialize() {
        Driver.add(new DriverDrawBridge());
    }
}
