package li.cil.oc.driver.appeng;


import appeng.api.IAEItemStack;
import li.cil.oc.api.Driver;
import li.cil.oc.driver.IModHandler;
import li.cil.oc.driver.Registry;
import net.minecraft.item.ItemStack;

import java.util.Map;

public final class HandlerAppEng implements IModHandler {
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
    public void populate(Map<String, Object> map, ItemStack stack) {
    }

    static Map<String, Object> toMap(IAEItemStack stack) {
        // TODO Do we want to add more (isCraftable?) here? If not, inline.
        // (Do we even need the 'requestable'?)
        Map<String, Object> map = Registry.toMap(stack.getItemStack());
        map.put("requestable", stack.getCountRequestable());
        return map;
    }
}
