package li.cil.oc.driver.buildcraft;

import li.cil.oc.api.Driver;
import li.cil.oc.driver.IModHandler;
import net.minecraft.item.ItemStack;

import java.util.Map;

public final class HandlerBuildCraft implements IModHandler {
    @Override
    public String getModId() {
        return "BuildCraft|Core";
    }

    @Override
    public void initialize() {
        Driver.add(new DriverPipe());
        Driver.add(new DriverPipeTE());
        Driver.add(new DriverPowerReceptor());
        Driver.add(new DriverMachine());
    }

    @Override
    public void populate(final Map<String, Object> map, final ItemStack stack) {
    }
}
