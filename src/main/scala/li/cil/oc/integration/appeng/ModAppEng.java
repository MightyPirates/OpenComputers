package li.cil.oc.integration.appeng;

import li.cil.oc.api.Driver;
import li.cil.oc.integration.IMod;
import li.cil.oc.integration.util.Mods;

public class ModAppEng implements IMod {
    @Override
    public Mods.Mod getMod() {
        return Mods.AppliedEnergistics2();
    }

    @Override
    public void initialize() {
        Driver.add(new DriverGridNode());
        Driver.add(new DriverCellContainer());

        Driver.add(new ConverterCellInventory());
    }
}
