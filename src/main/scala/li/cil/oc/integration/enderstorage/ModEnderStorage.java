package li.cil.oc.integration.enderstorage;

import li.cil.oc.api.Driver;
import li.cil.oc.integration.IMod;
import li.cil.oc.util.mods.Mods;

public final class ModEnderStorage implements IMod {
    @Override
    public Mods.Mod getMod() {
        return Mods.EnderStorage();
    }

    @Override
    public void initialize() {
        Driver.add(new DriverFrequencyOwner());
    }
}
