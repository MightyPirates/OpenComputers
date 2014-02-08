package li.cil.occ.mods.enderstorage;

import li.cil.oc.api.Driver;
import li.cil.occ.mods.IMod;
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
