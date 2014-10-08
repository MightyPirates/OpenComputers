package li.cil.oc.integration.buildcraft;

import li.cil.oc.api.Driver;
import li.cil.oc.integration.IMod;
import li.cil.oc.util.mods.Mods;

public final class ModBuildCraft implements IMod {
    @Override
    public Mods.Mod getMod() {
        return Mods.BuildCraft();
    }

    @Override
    public void initialize() {
        Driver.add(new DriverPipeTE());
        Driver.add(new DriverPowerReceptor());
        Driver.add(new DriverMachine());
    }
}
