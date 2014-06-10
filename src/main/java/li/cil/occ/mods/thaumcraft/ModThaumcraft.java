package li.cil.occ.mods.thaumcraft;

import li.cil.oc.api.Driver;
import li.cil.occ.mods.IMod;

public class ModThaumcraft implements IMod {
    public static final String MOD_ID = "Thaumcraft";

    @Override
    public String getModId() {
        return MOD_ID;
    }

    @Override
    public void initialize() {
        Driver.add(new ConverterIAspectContainer());
        Driver.add(new DriverAspectContainer());
    }
}
