package li.cil.oc.driver.computercraft;

import li.cil.oc.OpenComponents;
import li.cil.oc.api.Driver;
import li.cil.oc.driver.IModHandler;
import net.minecraft.item.ItemStack;

import java.util.Map;

public final class HandlerComputerCraft implements IModHandler {
    @Override
    public String getModId() {
        return "ComputerCraft";
    }

    @Override
    public void initialize() {
        if (OpenComponents.computerCraftWrapEverything) {
            Driver.add(new DriverPeripheral());
        }
    }

    @Override
    public void populate(final Map<String, Object> map, final ItemStack stack) {
    }
}
