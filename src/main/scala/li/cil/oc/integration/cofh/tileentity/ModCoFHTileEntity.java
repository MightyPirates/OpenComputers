package li.cil.oc.integration.cofh.tileentity;

import li.cil.oc.api.Driver;
import li.cil.oc.integration.IMod;
import li.cil.oc.integration.Mods;

public final class ModCoFHTileEntity implements IMod {
    @Override
    public Mods.Mod getMod() {
        return Mods.CoFHTileEntity();
    }

    @Override
    public void initialize() {
        Driver.add(new DriverEnergyInfo());
        Driver.add(new DriverRedstoneControl());
        Driver.add(new DriverSecureTile());
    }
}
