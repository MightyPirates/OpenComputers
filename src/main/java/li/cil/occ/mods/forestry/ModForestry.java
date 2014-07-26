package li.cil.occ.mods.forestry;

import li.cil.oc.api.Driver;
import li.cil.occ.mods.IMod;

public class ModForestry implements IMod {
    public static final String MOD_ID = "Forestry";

    @Override
    public String getModId() {
        return MOD_ID;
    }

    @Override
    public void initialize() {
        Driver.add(new ConverterIAlleles());
        Driver.add(new ConverterIIndividual());
        Driver.add(new DriverBeeHouse());
    }
}
