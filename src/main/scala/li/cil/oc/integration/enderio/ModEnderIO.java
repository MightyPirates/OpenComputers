package li.cil.oc.integration.enderio;

import li.cil.oc.api.Driver;
import li.cil.oc.integration.IMod;
import li.cil.oc.util.mods.Mods;

public final class ModEnderIO implements IMod {
    @Override
    public Mods.Mod getMod() {
        return Mods.EnderIO();
    }

    @Override
    public void initialize() {
        Driver.add(new DriverCapacitor());
    }
}
