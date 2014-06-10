package li.cil.occ.mods.buildcraft;

import li.cil.oc.api.Driver;
import li.cil.occ.mods.IMod;

public final class ModBuildCraft implements IMod {
    public static final String MOD_ID = "BuildCraft|Core";

    @Override
    public String getModId() {
        return MOD_ID;
    }

    @Override
    public void initialize() {
        Driver.add(new DriverPipeTE());
        Driver.add(new DriverPowerReceptor());
        Driver.add(new DriverMachine());
    }
}
