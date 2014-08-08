package li.cil.occ.mods.thermalexpansion;

import li.cil.oc.api.Driver;
import li.cil.occ.mods.IMod;

public final class ModThermalExpansion implements IMod {
    public static final String MOD_ID = "CoFHLib";

    @Override
    public String getModId() {
        return MOD_ID;
    }

    @Override
    public void initialize() {
        Driver.add(new DriverEnderEnergy());
        Driver.add(new DriverEnderFluid());
        Driver.add(new DriverEnderItem());
        Driver.add(new DriverEnergyHandler());
        Driver.add(new DriverEnergyInfo());
        Driver.add(new DriverLamp());
        Driver.add(new DriverRedstoneControl());
        Driver.add(new DriverSecureTile());

        Driver.add(new ConverterEnergyContainerItem());
    }
}
