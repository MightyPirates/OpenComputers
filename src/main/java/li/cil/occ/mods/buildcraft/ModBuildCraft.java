package li.cil.occ.mods.buildcraft;

import li.cil.oc.api.Driver;
import li.cil.occ.mods.IMod;
import net.minecraft.item.ItemStack;

import java.util.Map;

public final class ModBuildCraft implements IMod {
    @Override
    public String getModId() {
        return "BuildCraft|Core";
    }

    @Override
    public void initialize() {
        Driver.add(new DriverPipeTE());
        Driver.add(new DriverPowerReceptor());
        Driver.add(new DriverMachine());
    }

    @Override
    public void populate(final Map<String, Object> map, final ItemStack stack) {
    }
}
