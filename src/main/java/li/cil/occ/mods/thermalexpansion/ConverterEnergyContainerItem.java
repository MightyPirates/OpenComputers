package li.cil.occ.mods.thermalexpansion;

import cofh.api.energy.IEnergyContainerItem;
import li.cil.oc.api.driver.Converter;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.Map;

public class ConverterEnergyContainerItem implements Converter {
    @Override
    public void convert(final Object value, final Map<Object, Object> output) {
        if (value instanceof ItemStack) {
            final ItemStack stack = (ItemStack) value;
            final Item item = stack.getItem();
            if (item instanceof IEnergyContainerItem) {
                final IEnergyContainerItem energyItem = (IEnergyContainerItem) item;
                output.put("energy", energyItem.getEnergyStored(stack));
                output.put("maxEnergy", energyItem.getMaxEnergyStored(stack));
            }
        }
    }
}
