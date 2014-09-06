package li.cil.occ.mods.cofh.tileentity;

import li.cil.oc.api.Driver;
import li.cil.occ.mods.IMod;

public final class ModCoFHTileEntity implements IMod {
    public static final String MOD_ID = "CoFHAPI|tileentity";

    @Override
    public String getModId() {
        return MOD_ID;
    }

    @Override
    public void initialize() {
        Driver.add(new DriverEnergyInfo());
        Driver.add(new DriverRedstoneControl());
        Driver.add(new DriverSecureTile());
    }
}
