package li.cil.occ.mods.mystcraft;

import li.cil.oc.api.Driver;
import li.cil.occ.mods.IMod;

public class ModMystcraft implements IMod {
    public static final String MOD_ID = "Mystcraft";

    @Override
    public String getModId() {
        return MOD_ID;
    }

    @Override
    public void initialize() {
        Driver.add(new ConverterAgebook());
        Driver.add(new ConverterLinkbook());
        Driver.add(new ConverterPage());
    }
}
