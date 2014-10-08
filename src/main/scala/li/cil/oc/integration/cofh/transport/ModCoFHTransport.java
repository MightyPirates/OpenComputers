package li.cil.oc.integration.cofh.transport;

import li.cil.oc.api.Driver;
import li.cil.oc.integration.IMod;
import li.cil.oc.util.mods.Mods;

public final class ModCoFHTransport implements IMod {
    @Override
    public Mods.Mod getMod() {
        return Mods.CoFHTransport();
    }

    @Override
    public void initialize() {
        Driver.add(new DriverEnderEnergy());
        Driver.add(new DriverEnderFluid());
        Driver.add(new DriverEnderItem());
    }
}
