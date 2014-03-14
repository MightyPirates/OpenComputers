package li.cil.occ.mods.atomicscience;

import li.cil.oc.api.Driver;
import li.cil.occ.mods.IMod;

public final class ModAtomicScience implements IMod {
    @Override
    public String getModId() {
        return "AtomicScience";
    }

    @Override
    public void initialize() {
        Driver.add(new DriverReactor());
        Driver.add(new DriverTemperature());
    }
}