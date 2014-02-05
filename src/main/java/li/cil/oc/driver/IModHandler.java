package li.cil.oc.driver;

import net.minecraft.item.ItemStack;

import java.util.Map;

public interface IModHandler {
    String getModId();

    void initialize();

    void populate(Map<String, Object> map, ItemStack stack);
}
