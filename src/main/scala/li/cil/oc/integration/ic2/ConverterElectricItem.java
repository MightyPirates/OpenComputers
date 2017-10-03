package li.cil.oc.integration.ic2;

import ic2.api.item.ElectricItem;
import ic2.api.item.IElectricItem;
import li.cil.oc.api.driver.Converter;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.Map;

public class ConverterElectricItem implements Converter {
    @Override
    public void convert(final Object value, final Map<Object, Object> output) {
        if (value instanceof ItemStack) {
            final ItemStack stack = (ItemStack) value;
            final Item item = stack.getItem();
            if (item instanceof IElectricItem) {
                final IElectricItem electricItem = (IElectricItem) item;
                output.put("canProvideEnergy", electricItem.canProvideEnergy(stack));
                output.put("charge", ElectricItem.manager.getCharge(stack));
                output.put("maxCharge", electricItem.getMaxCharge(stack));
                output.put("tier", electricItem.getTier(stack));
                output.put("transferLimit", electricItem.getTransferLimit(stack));
            }
        }
    }
}
