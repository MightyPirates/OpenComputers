package li.cil.occ.mods.cofh.energy;

import li.cil.oc.api.Driver;
import li.cil.occ.mods.IMod;

public final class ModCoFHEnergy implements IMod {
    public static final String MOD_ID = "CoFHAPI|energy";

    @Override
    public String getModId() {
        return MOD_ID;
    }

    @Override
    public void initialize() {
        Driver.add(new DriverEnergyHandler());

        Driver.add(new ConverterEnergyContainerItem());
    }
}
