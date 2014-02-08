package li.cil.occ.handler;

import net.minecraft.item.ItemStack;

import java.util.Map;

public interface IHandler {
    String getModId();

    void initialize();

    void populate(Map<String, Object> map, ItemStack stack);
}
