package li.cil.oc.integration.gregtech;

import li.cil.oc.api.Driver;
import li.cil.oc.integration.IMod;
import li.cil.oc.util.mods.Mods;

public final class ModGregtech implements IMod {
    @Override
    public Mods.Mod getMod() {
        return Mods.GregTech();
    }

    @Override
    public void initialize() {
        Driver.add(new DriverEnergyContainer());
    }
}
