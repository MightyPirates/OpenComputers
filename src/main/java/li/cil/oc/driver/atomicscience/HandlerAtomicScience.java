package li.cil.oc.driver.atomicscience;


import li.cil.oc.api.Driver;
import li.cil.oc.driver.IModHandler;
import net.minecraft.item.ItemStack;

import java.util.Map;

public class HandlerAtomicScience implements IModHandler {
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