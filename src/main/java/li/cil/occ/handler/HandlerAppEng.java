package li.cil.occ.handler;


import appeng.api.IAEItemStack;
import li.cil.oc.api.Driver;
import li.cil.occ.handler.appeng.DriverCellProvider;
import li.cil.occ.handler.appeng.DriverGridTileEntity;
import li.cil.occ.handler.appeng.DriverTileController;
import net.minecraft.item.ItemStack;

import java.util.Map;

public final class HandlerAppEng implements IHandler {
    @Override
    public String getModId() {
        return "AppliedEnergistics";
    }

    @Override
    public void initialize() {
        Driver.add(new DriverCellProvider());
        Driver.add(new DriverGridTileEntity());
        Driver.add(new DriverTileController());
    }

    @Override
    public void populate(final Map<String, Object> map, final ItemStack stack) {
    }

    public static Map<String, Object> toMap(final IAEItemStack stack) {
        // TODO Do we want to add more (isCraftable?) here? If not, inline.
        // (Do we even need the 'requestable'?)
        final Map<String, Object> map = Registry.toMap(stack.getItemStack());
        map.put("requestable", stack.getCountRequestable());
        return map;
    }
}
