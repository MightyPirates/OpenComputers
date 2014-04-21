package li.cil.occ.mods.appeng;

import li.cil.oc.api.Driver;
import li.cil.occ.mods.IMod;

public class ModAppEng implements IMod {
    @Override
    public String getModId() {
        return "appliedenergistics2";
    }

    @Override
    public void initialize() {
        Driver.add(new DriverGridNode());
        Driver.add(new DriverCellContainer());

        Driver.add(new ConverterCellInventory());
    }
}
