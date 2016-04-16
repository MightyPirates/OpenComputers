package li.cil.oc.integration.mystcraft;

import li.cil.oc.api.driver.Converter;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.util.HashMap;
import java.util.Map;

public class ConverterAgebook implements Converter {
    @Override
    public void convert(final Object value, final Map<Object, Object> output) {
        if (value instanceof ItemStack) {
            final ItemStack stack = (ItemStack) value;
            if ("item.myst.agebook".equals(stack.getUnlocalizedName()) && stack.hasTagCompound()) {
                final NBTTagCompound tag = stack.getTagCompound();
                if (tag.hasKey("Dimension"))
                    output.put("dimensionId", tag.getInteger("Dimension"));
                if (tag.hasKey("DisplayName"))
                    output.put("dimensionName", tag.getString("DisplayName"));

                if (tag.hasKey("Flags")) {
                    final HashMap<String, Boolean> flags = new HashMap<String, Boolean>();
                    output.put("flags", flags);

                    final NBTTagCompound flagsNbt = tag.getCompoundTag("Flags");
                    for (String key : flagsNbt.getKeySet()) {
                        flags.put(key, flagsNbt.getBoolean(key));
                    }
                }
            }
        }
    }
}
