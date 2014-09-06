package li.cil.occ.mods.cofh.transport;

import li.cil.oc.api.Driver;
import li.cil.occ.mods.IMod;

public final class ModCoFHTransport implements IMod {
    public static final String MOD_ID = "CoFHAPI|transport";

    @Override
    public String getModId() {
        return MOD_ID;
    }

    @Override
    public void initialize() {
        Driver.add(new DriverEnderEnergy());
        Driver.add(new DriverEnderFluid());
        Driver.add(new DriverEnderItem());
    }
}
