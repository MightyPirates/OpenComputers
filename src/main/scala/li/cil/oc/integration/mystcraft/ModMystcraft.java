package li.cil.oc.integration.mystcraft;

import li.cil.oc.api.Driver;
import li.cil.oc.integration.IMod;
import li.cil.oc.integration.Mods;

public class ModMystcraft implements IMod {
    @Override
    public Mods.Mod getMod() {
        return Mods.Mystcraft();
    }

    @Override
    public void initialize() {
        Driver.add(new ConverterAgebook());
        Driver.add(new ConverterLinkbook());
        Driver.add(new ConverterPage());
    }
}
