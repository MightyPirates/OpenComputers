package li.cil.oc.integration.thermalexpansion;

import li.cil.oc.api.Driver;
import li.cil.oc.integration.IMod;
import li.cil.oc.util.mods.Mods;

public final class ModThermalExpansion implements IMod {
    @Override
    public Mods.Mod getMod() {
        return Mods.ThermalExpansion();
    }

    @Override
    public void initialize() {
        Driver.add(new DriverLamp());
    }
}
