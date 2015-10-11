package li.cil.oc.integration.rotarycraft;

import cpw.mods.fml.common.registry.GameRegistry;
import li.cil.oc.api.driver.Converter;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.util.Map;

public class ConverterPumpItem implements Converter {
    final Item Pump = GameRegistry.findItem("RotaryCraft", "rotarycraft_item_pump");

    @Override
    public void convert(final Object value, final Map<Object, Object> output) {
        if (value instanceof ItemStack) {
            final ItemStack stack = (ItemStack) value;
            final Item item = stack.getItem();
            if (item != null && item == Pump) {
                final NBTTagCompound tag = stack.getTagCompound();

                if (tag != null && tag.hasKey("liquid")) {
                    output.put("liquid", stack.stackTagCompound.getString("liquid"));
                } else {
                    output.put("liquid", "empty");
                }

                if (tag != null && tag.hasKey("lvl")) {
                    output.put("fluidAmount", stack.stackTagCompound.getInteger("lvl"));
                } else {
                    output.put("fluidAmount", 0);
                }
            }
        }
    }
}
