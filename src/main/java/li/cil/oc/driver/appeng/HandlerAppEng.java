package li.cil.oc.driver.appeng;


import appeng.api.IAEItemStack;
import li.cil.oc.api.Driver;
import li.cil.oc.driver.IModHandler;
import net.minecraft.item.ItemStack;

import java.util.HashMap;
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

    public static Map<String, Object> toMap(IAEItemStack stack) {
        if (stack == null) {
            return null;
        }
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put("id", stack.getItemID());
        map.put("damage", stack.getItemDamage());
        map.put("size", stack.getStackSize());
        map.put("hasTag", stack.hasTagCompound());
        map.put("name", stack.getItemStack().getUnlocalizedName());
        map.put("requestable", stack.getCountRequestable());

        if (stack.getItemStack().getDisplayName() != null) {
            map.put("label", stack.getItemStack().getDisplayName());
        }
        return map;
    }

    @Override
    public void populate(Map<String, Object> map, ItemStack stack) {
    }
}
