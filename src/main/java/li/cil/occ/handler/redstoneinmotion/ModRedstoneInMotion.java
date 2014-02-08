package li.cil.occ.handler.redstoneinmotion;

import li.cil.oc.api.Driver;
import li.cil.occ.handler.IMod;
import li.cil.occ.handler.redstoneinmotion.DriverCarriageController;
import net.minecraft.item.ItemStack;

import java.util.Map;

public final class ModRedstoneInMotion implements IMod {
    @Override
    public String getModId() {
        return "JAKJ_RedstoneInMotion";
    }

    @Override
    public void initialize() {
        Driver.add(new DriverCarriageController());
    }

    @Override
    public void populate(final Map<String, Object> map, final ItemStack stack) {
    }
}
