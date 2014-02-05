package li.cil.oc.util;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidTankInfo;

import java.util.HashMap;
import java.util.Map;

public final class TypeConversion {
    private TypeConversion() {
    }

    public static Map toMap(final ItemStack value) {
        if (value == null) {
            return null;
        }
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put("id", value.itemID);
        map.put("damage", value.getItemDamage());
        map.put("maxDamage", value.getMaxDamage());
        map.put("size", value.stackSize);
        map.put("maxSize", value.getMaxStackSize());
        map.put("hasTag", value.hasTagCompound());
        return map;
    }

    public static Map toMap(final FluidTankInfo value) {
        if (value == null) {
            return null;
        }
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put("capacity", value.capacity);
        if (value.fluid != null) {
            map.put("amount", value.fluid.amount);
            map.put("id", value.fluid.fluidID);
            final Fluid fluid = value.fluid.getFluid();
            if (fluid != null) {
                map.put("name", fluid.getName());
                map.put("label", fluid.getLocalizedName());
            }
        } else {
            map.put("amount", 0);
        }
        return map;
    }
}
