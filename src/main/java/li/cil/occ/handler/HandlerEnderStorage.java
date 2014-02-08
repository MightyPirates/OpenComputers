package li.cil.occ.handler;

import li.cil.oc.api.Driver;
import li.cil.occ.handler.enderstorage.DriverFrequencyOwner;
import net.minecraft.item.ItemStack;

import java.util.Map;

public final class HandlerEnderStorage implements IHandler {
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
