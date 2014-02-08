package li.cil.occ.handler;

import li.cil.oc.api.Driver;
import li.cil.occ.handler.buildcraft.DriverMachine;
import li.cil.occ.handler.buildcraft.DriverPipe;
import li.cil.occ.handler.buildcraft.DriverPipeTE;
import li.cil.occ.handler.buildcraft.DriverPowerReceptor;
import net.minecraft.item.ItemStack;

import java.util.Map;

public final class HandlerBuildCraft implements IHandler {
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
