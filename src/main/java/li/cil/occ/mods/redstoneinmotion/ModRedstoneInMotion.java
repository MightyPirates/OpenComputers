package li.cil.occ.mods.redstoneinmotion;

import li.cil.oc.api.Driver;
import li.cil.occ.mods.IMod;

public final class ModRedstoneInMotion implements IMod {
    @Override
    public String getModId() {
        return "JAKJ_RedstoneInMotion";
    }

    @Override
    public void initialize() {
        Driver.add(new DriverCarriageController());
    }
}
