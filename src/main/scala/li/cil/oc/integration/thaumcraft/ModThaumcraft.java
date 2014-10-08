package li.cil.oc.integration.thaumcraft;

import li.cil.oc.api.Driver;
import li.cil.oc.integration.IMod;
import li.cil.oc.util.mods.Mods;

public class ModThaumcraft implements IMod {
    @Override
    public Mods.Mod getMod() {
        return Mods.Thaumcraft();
    }

    @Override
    public void initialize() {
        Driver.add(new DriverAspectContainer());

        Driver.add(new ConverterIAspectContainer());
    }
}
