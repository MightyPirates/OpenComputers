package li.cil.oc.integration.cofh.energy;

import li.cil.oc.api.Driver;
import li.cil.oc.integration.IMod;
import li.cil.oc.util.mods.Mods;

public final class ModCoFHEnergy implements IMod {
    @Override
    public Mods.Mod getMod() {
        return Mods.CoFHEnergy();
    }

    @Override
    public void initialize() {
        Driver.add(new DriverEnergyHandler());

        Driver.add(new ConverterEnergyContainerItem());
    }
}
