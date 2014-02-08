package li.cil.occ.handler;

import li.cil.oc.api.Driver;
import li.cil.occ.handler.redstoneinmotion.DriverCarriageController;
import net.minecraft.item.ItemStack;

import java.util.Map;

public final class HandlerRedstoneInMotion implements IHandler {
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
