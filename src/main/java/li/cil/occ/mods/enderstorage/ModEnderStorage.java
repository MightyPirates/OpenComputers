package li.cil.occ.mods.enderstorage;

import li.cil.oc.api.Driver;
import li.cil.occ.mods.IMod;

public final class ModEnderStorage implements IMod {
    @Override
    public String getModId() {
        return "EnderStorage";
    }

    @Override
    public void initialize() {
        Driver.add(new DriverFrequencyOwner());
    }
}
