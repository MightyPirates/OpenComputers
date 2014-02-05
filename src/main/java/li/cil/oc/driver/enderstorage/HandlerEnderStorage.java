package li.cil.oc.driver.enderstorage;

import li.cil.oc.api.Driver;
import li.cil.oc.driver.IModHandler;
import net.minecraft.item.ItemStack;

import java.util.Map;

public final class HandlerEnderStorage implements IModHandler {
    @Override
    public String getModId() {
        return "EnderStorage";
    }

    @Override
    public void initialize() {
        Driver.add(new DriverFrequencyOwner());
    }

    @Override
    public void populate(final Map<String, Object> map, final ItemStack stack) {
    }
}
