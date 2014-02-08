package li.cil.oc.driver.mekanism;

import li.cil.oc.api.Driver;
import li.cil.oc.driver.IModHandler;
import net.minecraft.item.ItemStack;

import java.util.Map;

public class HandlerMekanism  implements IModHandler {
    @Override
    public String getModId() {
        return "Mekanism";
    }

    @Override
    public void initialize() {
        Driver.add(new DriverEnergyCube());
        Driver.add(new DriverGenerator());
    }

    @Override
    public void populate(Map<String, Object> map, ItemStack stack) {

    }
}
