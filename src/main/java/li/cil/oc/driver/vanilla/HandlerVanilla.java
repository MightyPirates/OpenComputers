package li.cil.oc.driver.vanilla;

import li.cil.oc.api.Driver;
import li.cil.oc.driver.IModHandler;
import net.minecraft.item.ItemStack;

import java.util.Map;

public final class HandlerVanilla implements IModHandler {
    @Override
    public String getModId() {
        return null;
    }

    @Override
    public void initialize() {
        Driver.add(new DriverBeacon());
        Driver.add(new DriverBrewingStand());
        Driver.add(new DriverComparator());
        Driver.add(new DriverFluidHandler());
        Driver.add(new DriverFluidTank());
        Driver.add(new DriverFurnace());
        Driver.add(new DriverInventory());
        Driver.add(new DriverMobSpawner());
        Driver.add(new DriverRecordPlayer());
        Driver.add(new DriverSign());
    }

    @Override
    public void populate(final Map<String, Object> map, final ItemStack stack) {
        map.put("id", stack.itemID);
        map.put("damage", stack.getItemDamage());
        map.put("maxDamage", stack.getMaxDamage());
        map.put("size", stack.stackSize);
        map.put("maxSize", stack.getMaxStackSize());
        map.put("hasTag", stack.hasTagCompound());
    }
}
