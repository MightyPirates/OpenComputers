package li.cil.occ.handler;

import li.cil.oc.api.Driver;
import li.cil.occ.OpenComponents;
import li.cil.occ.handler.computercraft.DriverPeripheral;
import net.minecraft.item.ItemStack;

import java.util.Map;

public final class HandlerComputerCraft implements IHandler {
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
