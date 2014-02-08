package li.cil.occ.handler.atomicscience;

import li.cil.oc.api.Driver;
import li.cil.occ.handler.IMod;
import li.cil.occ.handler.atomicscience.DriverReactor;
import li.cil.occ.handler.atomicscience.DriverTemperature;
import net.minecraft.item.ItemStack;

import java.util.Map;

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

    @Override
    public void populate(final Map<String, Object> map, final ItemStack stack) {
    }
}