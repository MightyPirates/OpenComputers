package li.cil.oc.integration.forestry;

import li.cil.oc.api.Driver;
import li.cil.oc.integration.IMod;
import li.cil.oc.util.mods.Mods;

public class ModForestry implements IMod {
    @Override
    public Mods.Mod getMod() {
        return Mods.Forestry();
    }

    @Override
    public void initialize() {
        Driver.add(new ConverterIAlleles());
        Driver.add(new ConverterIIndividual());
        Driver.add(new DriverBeeHouse());
    }
}
