package li.cil.occ.mods.appeng;


import li.cil.oc.api.Driver;
import li.cil.occ.mods.IMod;

public final class ModAppEng implements IMod {
    public static final String MOD_ID = "AppliedEnergistics";

    @Override
    public String getModId() {
        return MOD_ID;
    }

    @Override
    public void initialize() {
        Driver.add(new DriverCellProvider());
        Driver.add(new DriverGridTileEntity());
        Driver.add(new DriverTileController());
    }
}
