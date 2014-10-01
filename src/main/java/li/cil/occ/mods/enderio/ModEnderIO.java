package li.cil.occ.mods.enderio;

import li.cil.oc.api.Driver;
import li.cil.occ.mods.IMod;

public final class ModEnderIO implements IMod {
    public static final String MOD_ID = "EnderIO";

    @Override
    public String getModId() {
        return MOD_ID;
    }

    @Override
    public void initialize() {
        Driver.add(new DriverCapacitor());
    }
}
