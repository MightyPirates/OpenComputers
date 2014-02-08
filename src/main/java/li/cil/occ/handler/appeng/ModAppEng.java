package li.cil.occ.handler.appeng;


import appeng.api.IAEItemStack;
import li.cil.oc.api.Driver;
import li.cil.occ.handler.IMod;
import li.cil.occ.handler.Registry;
import net.minecraft.item.ItemStack;

import java.util.Map;

public final class ModAppEng implements IMod {
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

    static Map<String, Object> toMap(final IAEItemStack stack) {
        // TODO Do we want to add more (isCraftable?) here? If not, inline.
        // (Do we even need the 'requestable'?)
        final Map<String, Object> map = Registry.toMap(stack.getItemStack());
        map.put("requestable", stack.getCountRequestable());
        return map;
    }
}
