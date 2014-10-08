package li.cil.occ.mods.enderstorage;

import li.cil.oc.api.Driver;
import li.cil.occ.mods.IMod;

public final class ModEnderStorage implements IMod {
    public static final String MOD_ID = "EnderStorage";

    @Override
    public String getModId() {
        return MOD_ID;
    }

    @Override
    public void initialize() {
        Driver.add(new DriverFrequencyOwner());
    }
}
