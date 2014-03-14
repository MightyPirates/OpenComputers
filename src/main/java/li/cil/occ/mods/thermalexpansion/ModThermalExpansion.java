package li.cil.occ.mods.thermalexpansion;

import li.cil.oc.api.Driver;
import li.cil.occ.mods.IMod;

public final class ModThermalExpansion implements IMod {
    @Override
    public String getModId() {
        return "ThermalExpansion";
    }

    @Override
    public void initialize() {
        Driver.add(new DriverEnderAttuned());
        Driver.add(new DriverEnergyHandler());
        Driver.add(new DriverEnergyInfo());
        Driver.add(new DriverLamp());
        Driver.add(new DriverRedstoneControl());
        Driver.add(new DriverSecureTile());

        Driver.add(new ConverterEnergyContainerItem());
    }
}
