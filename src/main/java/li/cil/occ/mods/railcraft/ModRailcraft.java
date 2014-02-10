package li.cil.occ.mods.railcraft;

import li.cil.oc.api.Driver;
import li.cil.occ.mods.IMod;
import net.minecraft.item.ItemStack;

import java.util.Map;

public final class ModRailcraft implements IMod {
    @Override
    public String getModId() {
        return "Railcraft";
    }

    @Override
    public void initialize() {
        Driver.add(new DriverSteamTurbine());
    }

    @Override
    public void populate(final Map<String, Object> map, final ItemStack stack) {
    }
}
