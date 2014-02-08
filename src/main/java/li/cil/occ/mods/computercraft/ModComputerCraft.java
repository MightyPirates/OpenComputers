package li.cil.occ.mods.computercraft;

import li.cil.oc.api.Driver;
import li.cil.occ.mods.IMod;
import net.minecraft.item.ItemStack;

import java.util.Map;

public final class ModComputerCraft implements IMod {
    @Override
    public String getModId() {
        return "ComputerCraft";
    }

    @Override
    public void initialize() {
        Driver.add(new DriverPeripheral());
    }

    @Override
    public void populate(final Map<String, Object> map, final ItemStack stack) {
    }
}
