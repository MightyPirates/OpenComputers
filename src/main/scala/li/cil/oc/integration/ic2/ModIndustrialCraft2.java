package li.cil.oc.integration.ic2;

import li.cil.oc.api.Driver;
import li.cil.oc.integration.IMod;
import li.cil.oc.util.mods.Mods;

public final class ModIndustrialCraft2 implements IMod {
    @Override
    public Mods.Mod getMod() {
        return Mods.IndustrialCraft2();
    }

    @Override
    public void initialize() {
        Driver.add(new DriverEnergyConductor());
        Driver.add(new DriverEnergySink());
        Driver.add(new DriverEnergySource());
        Driver.add(new DriverEnergyStorage());
        Driver.add(new DriverMassFab());
        Driver.add(new DriverReactor());
        Driver.add(new DriverReactorChamber());

        Driver.add(new ConverterElectricItem());
    }
}
