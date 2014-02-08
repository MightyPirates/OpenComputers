package li.cil.occ.handler.enderstorage;

import li.cil.oc.api.Driver;
import li.cil.occ.handler.IMod;
import li.cil.occ.handler.enderstorage.DriverFrequencyOwner;
import net.minecraft.item.ItemStack;

import java.util.Map;

public final class ModEnderStorage implements IMod {
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
