package li.cil.occ.mods.mystcraft;

import li.cil.oc.api.driver.Converter;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.util.Map;

public class ConverterPage implements Converter {
    @Override
    public void convert(final Object value, final Map<Object, Object> output) {
        if (value instanceof ItemStack) {
            final ItemStack stack = (ItemStack) value;
            if ("item.myst.page".equals(stack.getUnlocalizedName()) && stack.hasTagCompound()) {
                final NBTTagCompound tag = stack.getTagCompound();
                output.put("symbol", tag.getString("symbol"));
            }
        }
    }
}
