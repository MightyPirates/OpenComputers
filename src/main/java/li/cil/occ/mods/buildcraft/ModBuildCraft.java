package li.cil.occ.mods.buildcraft;

import li.cil.oc.api.Driver;
import li.cil.occ.mods.IMod;

public final class ModBuildCraft implements IMod {
    @Override
    public String getModId() {
        return "BuildCraft|Core";
    }

    @Override
    public void initialize() {
        Driver.add(new DriverPipeTE());
        Driver.add(new DriverPowerReceptor());
        Driver.add(new DriverMachine());
    }
}
