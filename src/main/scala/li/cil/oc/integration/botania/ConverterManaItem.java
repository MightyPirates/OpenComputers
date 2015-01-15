package li.cil.oc.integration.botania;

import li.cil.oc.api.driver.Converter;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import vazkii.botania.api.mana.IManaItem;

import java.util.Map;

public class ConverterManaItem implements Converter {
    @Override
    public void convert(Object value, Map<Object, Object> output) {
        if (value instanceof ItemStack) {
            final ItemStack stack = (ItemStack) value;
            final Item item = stack.getItem();
            if (item instanceof IManaItem) {
                final IManaItem manaItem = (IManaItem) item;
                output.put("mana", manaItem.getMana(stack));
                output.put("maxMana", manaItem.getMaxMana(stack));
            }
        }
    }
}
