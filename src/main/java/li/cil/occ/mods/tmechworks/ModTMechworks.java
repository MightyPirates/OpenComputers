package li.cil.occ.mods.tmechworks;

import li.cil.oc.api.Driver;
import li.cil.occ.mods.IMod;
import net.minecraft.item.ItemStack;

import java.util.Map;

public final class ModTMechworks implements IMod {
    @Override
    public String getModId() {
        return "TMechworks";
    }

    @Override
    public void initialize() {
        Driver.add(new DriverDrawBridge());
    }

    @Override
    public void populate(final Map<String, Object> map, final ItemStack stack) {
    }
}
