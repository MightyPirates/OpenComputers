package li.cil.occ.mods.redstoneinmotion;

import li.cil.oc.api.Driver;
import li.cil.occ.mods.IMod;

public final class ModRedstoneInMotion implements IMod {
    public static final String MOD_ID = "JAKJ_RedstoneInMotion";

    @Override
    public String getModId() {
        return MOD_ID;
    }

    @Override
    public void initialize() {
        Driver.add(new DriverCarriageController());
    }
}
